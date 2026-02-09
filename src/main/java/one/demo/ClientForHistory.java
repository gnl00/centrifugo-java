package one.demo;

import com.fasterxml.jackson.databind.json.JsonMapper;
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class ClientForHistory {

    private final static String TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJjZW50cmlmdWdvLWphdmEtZGVtbyIsInN1YiI6IjEwMDAxIiwiaWF0IjoxNzYyOTQwNTY5LCJleHAiOjE3NjM1NDUzNjl9.N8duYpzdCCWbKXh56sh3vjYKakakc3qRfn-DXrx3Rq4";

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
                        System.out.printf("received from channel:%s data:%s\n", sub.getChannel(), new String(event.getData(), Charset.defaultCharset()));
                    }

                    @Override
                    public void onSubscribed(Subscription sub, SubscribedEvent event) {
                        System.out.printf("subscribed %s \nrecovering? %s \n", "", event.wasRecovering());
                        // 从最新的 epoch 和 offset 开始取消息
                        // StreamPosition since = new StreamPosition(0, "");
                        // 从指定的 epoch 和 offset 开始取消息。取 epoch=yrrk 内 offset=4 之后的消息
                        // StreamPosition since = new StreamPosition(4, "yrrk");
                        // 此时 如果 Reverse = true，则取 offset=4 之前的消息，返回 offset=3,2,1 消息
                        // 此时 如果 Reverse = false，则取 offset=4 之后的消息，返回 offset=5,6,7 消息
                        StreamPosition since = null;
                        try {
                            byte[] bytes = Files.readAllBytes(Paths.get("./cf-message-history-record.json"));
                            if (bytes.length > 0) {
                                String hisJsonStr = new String(bytes);
                                JsonMapper mapper = new JsonMapper();
                                Map<String, Object> hisMap = mapper.readValue(hisJsonStr, Map.class);
                                System.out.printf("read local epoch %s offset %d\n", hisMap.get("epoch").toString(), Integer.parseInt(hisMap.get("offset").toString()));
                                since = new StreamPosition(Integer.parseInt(hisMap.get("offset").toString()), hisMap.get("epoch").toString());
                            }
                        } catch (IOException e) {
                            System.out.println("no cf-message-history-record.json found");
                        }
                        HistoryOptions historyOptions = new HistoryOptions.Builder()
                                .withLimit(-1)
                                .withSince(since)
                                .withReverse(false)
                                .build();
                        sub.history(historyOptions, new ResultCallback<>() {
                            @Override
                            public void onDone(Throwable e, HistoryResult result) {
                                if (null != e) {
                                    e.printStackTrace();
                                    return;
                                }
                                String epoch = result.getEpoch();
                                Long outerOffset = result.getOffset();
                                Long consumedOffset = -1L;
                                for (Publication publication : result.getPublications()) {
                                    System.out.printf("outer-offset:%d epoch:%s offset:%d publication:%s\n", outerOffset, epoch, publication.getOffset(), new String(publication.getData()));
                                    consumedOffset = publication.getOffset();
                                }
                                try {
                                    Files.writeString(Path.of("./cf-message-history-record.json"), String.format("{\"epoch\": \"%s\", \"offset\": %d}", epoch, consumedOffset), StandardOpenOption.CREATE);
                                    System.out.printf("refreshed local to epoch %s offset %d\n", epoch, consumedOffset);
                                } catch (IOException ex) {
                                    ex.printStackTrace();
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
