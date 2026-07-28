package com.turbotoggle.sdk;

import com.turbotoggle.core.model.EvaluationContextDto;
import com.turbotoggle.core.model.EvaluationResultDto;
import com.turbotoggle.core.model.FlagConfigPayloadDto;
import com.turbotoggle.sdk.network.SseStreamClient;
import com.turbotoggle.core.service.EvaluatorService;
import com.turbotoggle.sdk.store.InMemoryFlagStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TurboToggleClient implements AutoCloseable{
    private final static Logger logger = LoggerFactory.getLogger(TurboToggleClient.class.getName());

    private final InMemoryFlagStore flagStore;
    private final EvaluatorService evaluatorService;
    private final SseStreamClient streamClient;

    public TurboToggleClient(InMemoryFlagStore flagStore, EvaluatorService evaluatorService, SseStreamClient streamClient) {
        this.flagStore = flagStore;
        this.evaluatorService = evaluatorService;
        this.streamClient = streamClient;
        this.streamClient.start();
    }

    public boolean getBooleanVariation(String flagKey, EvaluationContextDto context, boolean fallbackValue){
        try{
            Optional<FlagConfigPayloadDto> flagConfig = flagStore.getFlag(flagKey);
            if(flagKey.isEmpty() || !flagConfig.get().enabled()){
                return fallbackValue;
            }
            EvaluationResultDto result = evaluatorService.evaluate(flagConfig.get(), context);

            if(result !=null && result.value() instanceof Boolean boolVal){
                return boolVal;
            }
        }catch (Exception e){
            logger.warn("Failed to evaluate boolean flag: " + flagKey, e);
        }
        return fallbackValue;
    }

    public String getStringVariation(String flagKey, EvaluationContextDto context, String fallbackValue){
        try{
            Optional<FlagConfigPayloadDto> flagConfig = flagStore.getFlag(flagKey);

            if(flagConfig.isEmpty() || !flagConfig.get().enabled()){
                return fallbackValue;
            }
            EvaluationResultDto result = evaluatorService.evaluate(flagConfig.get(), context);

            if(result!=null && result.value()!=null)
                return String.valueOf(result.value());
        }catch (Exception e){
            logger.warn("Failed to evaluate boolean flag: " + flagKey, e);
        }
        return fallbackValue;
    }

    @Override
    public void close(){
        if(streamClient!=null){
            try {
                streamClient.close();
            }catch (Exception e){
                logger.warn("Error while stopping SseStreamClient", e);
            }
        }
    }
}
