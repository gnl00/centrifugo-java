package one.demo;

import io.github.centrifugal.centrifuge.Client;
import io.github.centrifugal.centrifuge.ConnectedEvent;
import io.github.centrifugal.centrifuge.ConnectingEvent;
import io.github.centrifugal.centrifuge.DisconnectedEvent;
import io.github.centrifugal.centrifuge.DuplicateSubscriptionException;
import io.github.centrifugal.centrifuge.EventListener;
import io.github.centrifugal.centrifuge.HistoryOptions;
import io.github.centrifugal.centrifuge.HistoryResult;
import io.github.centrifugal.centrifuge.MessageEvent;
import io.github.centrifugal.centrifuge.Options;
import io.github.centrifugal.centrifuge.Publication;
import io.github.centrifugal.centrifuge.PublicationEvent;
import io.github.centrifugal.centrifuge.PublishResult;
import io.github.centrifugal.centrifuge.ResultCallback;
import io.github.centrifugal.centrifuge.StreamPosition;
import io.github.centrifugal.centrifuge.SubscribedEvent;
import io.github.centrifugal.centrifuge.Subscription;
import io.github.centrifugal.centrifuge.SubscriptionErrorEvent;
import io.github.centrifugal.centrifuge.SubscriptionEventListener;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;

public class AppForHistory {

    private final static String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM3MjIiLCJleHAiOjE3NjI4NDU4MjUsImlhdCI6MTc2MjI0MTAyNX0.ae9DSEeP2zJoxPAiMSq05K9MZRxiCVJclHkowc8mxr8";

    private static Client CLIENT = null;

    public static void main(String[] args) {
        new Thread(() -> {
            EventListener listener = new EventListener() {
                @Override
                public void onConnected(Client client, ConnectedEvent event) {
                    System.out.println("connected");
                }
                @Override
                public void onConnecting(Client client, ConnectingEvent event) {
                    System.out.printf("connecting: %s%n", event.getReason());
                }
                @Override
                public void onDisconnected(Client client, DisconnectedEvent event) {
                    System.out.printf("disconnected %d %s\n", event.getCode(), event.getReason());
                }

                @Override
                public void onMessage(Client client, MessageEvent event) {
                    System.out.printf("message %s\n", new String(event.getData()));
                }
            };

            Options opts = new Options();
            opts.setName("java-demo-2");
            opts.setToken(TOKEN);

            CLIENT = new Client("ws://localhost:8000/connection/websocket", opts, listener);
            Subscription subscription = null;
            try {
                subscription = CLIENT.newSubscription("channel", new SubscriptionEventListener() {

                    @Override
                    public void onPublication(Subscription sub, PublicationEvent event) {
                        System.out.printf("publication from channel:%s data:%s\n", sub.getChannel(), new String(event.getData(), Charset.defaultCharset()));
                    }

                    @Override
                    public void onSubscribed(Subscription sub, SubscribedEvent event) {
                        System.out.printf("subscribed %s\n", new String(event.getData()));
                        StreamPosition since = new StreamPosition(0, "Hibb");
                        HistoryOptions historyOptions = new HistoryOptions.Builder()
                                .withLimit(10)
                                //.withSince(since)
                                .withReverse(true)
                                .build();
                        sub.history(historyOptions, new ResultCallback<>() {
                            @Override
                            public void onDone(Throwable e, HistoryResult result) {
                                if (null != e) {
                                    e.printStackTrace();
                                    return;
                                }
                                String epoch = result.getEpoch();
                                Long offset = result.getOffset();
                                for (Publication publication : result.getPublications()) {
                                    System.out.printf("epoch:%s outer-offset:%d offset:%d publication:%s\n", epoch, offset, publication.getOffset(), new String(publication.getData()));
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(Subscription sub, SubscriptionErrorEvent event) {
                        System.out.printf("subscribe error %s\n", event.getError().getCause());
                    }
                });
            } catch (DuplicateSubscriptionException e) {
                throw new RuntimeException(e);
            }
            subscription.subscribe();
            CLIENT.connect();
        }).start();
        Scanner sc = new Scanner(System.in);
        System.out.println("started");
        while (sc.hasNext()) {
            String s = sc.nextLine();
            if ("send".equals(s)) {
                send();
            }
        }
    }

    /*==================================== SEND MESSAGE ==========================================*/
    public static void send() {
        String jsonStr = String.format("{\"value\": \"from-java-for-all\", \"timestamp\": %d}", System.currentTimeMillis());
        CLIENT.getSubscription("channel").publish(jsonStr.getBytes(StandardCharsets.UTF_8), new ResultCallback<PublishResult>() {
            @Override
            public void onDone(Throwable e, PublishResult result) {
                if (null != e) {
                    e.printStackTrace();
                    return;
                }
                System.out.printf("message published! result:%s\n", Objects.nonNull(result.toString()));
            }
        });
    }
}
