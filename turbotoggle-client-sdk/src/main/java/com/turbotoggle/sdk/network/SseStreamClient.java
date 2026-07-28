package com.turbotoggle.sdk.network;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turbotoggle.core.model.FlagConfigPayloadDto;
import com.turbotoggle.sdk.store.InMemoryFlagStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SseStreamClient implements AutoCloseable{
    private static final Logger log = LoggerFactory.getLogger(SseStreamClient.class);

    private final String serverUrl;
    private final String sdkKey;
    private final InMemoryFlagStore flagStore;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // Single-thread scheduled executor for backoff/reconnection timers
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();

    // Java 21 Virtual Thread Executor for concurrent stream reading and task execution
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Backoff Constants
    private static final long INITIAL_BACKOFF_MS = 1000; // 1s initial delay
    private static final long MAX_BACKOFF_MS = 30000;    // 30s max delay cap
    private final AtomicInteger retryAttempt = new AtomicInteger(0);
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    private final static int TIMEOUT_DURATION_SECONDS = 10;
    private final static String EVENT_MESSAGE_PREFIX = "event:";
    private final static String DATA_MESSAGE_PREFIX = "data:";
    private final static String X_SDK_KEY = "X-SDK-KEY";
    private final static String STREAM_API= "/api/v1/stream";
    private final static String INIT_EVENT_TYPE = "INIT";
    private final static String FLAG_MUTATED_EVENT_TYPE = "FLAG_MUTATED";

    public SseStreamClient(String serverUrl, String sdkKey, InMemoryFlagStore flagStore) {
        this.serverUrl = serverUrl;
        this.sdkKey = sdkKey;
        this.flagStore = flagStore;
        objectMapper = new ObjectMapper();
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(TIMEOUT_DURATION_SECONDS)).build();
    }

    public void start(){
        if(!isClosed.get()){
            connect();
        }
    }

    private void connect(){
        if(isClosed.get()){
            return;
        }
        try{
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + STREAM_API))
                    .header(X_SDK_KEY, sdkKey)
                    .GET()
                    .build();
            log.info("Connecting to TurboToggle SSE stream at {}...", serverUrl);

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenAccept(response ->{
                        if(response.statusCode()==200){
                            retryAttempt.set(0);
                            processStream(response.body());
                        }else{
                            log.error("Failed to connect to stream. HTTP status: {}", response.statusCode());
                            scheduleReconnect();
                        }
                    }).exceptionally(ex -> {
                        if(!isClosed.get()) {
                            log.error("Error reading SSE stream", ex);
                            scheduleReconnect();
                        }
                        return null;
                    });
        }catch (Exception e){
            if(!isClosed.get()) {
                log.error("Failed to initiate stream request", e);
                scheduleReconnect();
            }
        }
    }
    private void processStream(InputStream inputStream){
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))){
            String line;
            String currentEvent = null;
            while(!isClosed.get() && (line = reader.readLine())!=null){
                if (line.startsWith(EVENT_MESSAGE_PREFIX)) {
                    currentEvent = line.substring(EVENT_MESSAGE_PREFIX.length()).trim();
                } else if (line.startsWith(DATA_MESSAGE_PREFIX) && currentEvent != null) {
                    String dataJson = line.substring(DATA_MESSAGE_PREFIX.length()).trim();
                    String eventType = currentEvent;
                    currentEvent = null; // Reset for next event
                    // Process event payload concurrently on a Virtual Thread
                    virtualThreadExecutor.submit(() -> handleEvent(eventType, dataJson));
                }
            }
        }catch (Exception e){
            if(!isClosed.get()) {
                log.warn("SSE stream disconnected. Attempting to reconnect...", e);
                scheduleReconnect();
            }
        }
    }

    private void handleEvent(String eventType, String dataJson){
        try{
            if(INIT_EVENT_TYPE.equalsIgnoreCase(eventType)){
                List<FlagConfigPayloadDto> flags = objectMapper.readValue(dataJson, new TypeReference<>() {});
                flagStore.updateAll(flags);
                log.info("SDK initialized with {} flag configurations.", flags.size());
            }else if(FLAG_MUTATED_EVENT_TYPE.equalsIgnoreCase(eventType)){
                FlagConfigPayloadDto flag = objectMapper.readValue(dataJson, FlagConfigPayloadDto.class);
                flagStore.putFlag(flag);
                log.info("SDK updated flag [{}] to version [{}]", flag.flagKey(), flag.version());
            }
        }catch(Exception e){
            log.error("Error parsing event data for event [{}]", eventType, e);
        }
    }
    private void scheduleReconnect() {
        if(isClosed.get()){
            return;
        }
        int currentAttempts = retryAttempt.incrementAndGet();
        long delayMs = calculateBackoffWithJitter(currentAttempts);

        log.info("Scheduling SSE stream reconnection in {} ms (attempt #{})...", delayMs, currentAttempts);
        reconnectExecutor.schedule(this::connect, delayMs, TimeUnit.MILLISECONDS);
    }
    private long calculateBackoffWithJitter(int attemp){
        long exponentialDelay = (long)(Math.min(MAX_BACKOFF_MS, INITIAL_BACKOFF_MS*Math.pow(2,attemp-1)));
        double jitterFactor = 0.5 + ThreadLocalRandom.current().nextDouble();
        return (long)(exponentialDelay*jitterFactor);
    }

    @Override
    public void close() {
        if(isClosed.compareAndExchange(false, true)){
            log.info("Closing SseStreamClient and releasing resources...");
            reconnectExecutor.shutdownNow();
            virtualThreadExecutor.shutdownNow();
        }

    }
}
