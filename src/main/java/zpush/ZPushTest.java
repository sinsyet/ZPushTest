package zpush;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZPushTest {
    private static Scanner sScanner = new Scanner(System.in);
    private static final String SERVER = "119.23.149.16";
    private static final int PORT = 9800;
    private static BufferedReader reader;
    private static BufferedWriter writer;
    private static String alias;
    private static Integer frequency;

    private static ExecutorService threadPool = Executors.newCachedThreadPool();
    private static OutputStream os;
    private static boolean showHBLog;

    public static void main(String[] args) {
        alias = input(String.class, "set alias");
        frequency = input(Integer.class, "set heartbeat frequency");

        if (!connectServer()) {
            return;
        }


        threadPool.execute(readerRunnable);
        threadPool.execute(heartBeatRunnable);
        showNotices();
        sleep(1000);
        System.out.println("--------------------------------------------");
        while (true) {

            String command = input(String.class, "input command");
            switch (command.toLowerCase()) {
                case "register":
                    register();
                    break;
                case "get_topic":
                    get_topic();
                    break;
                case "pull_msg":
                    pull_msg();
                    break;
                case "push_to_alias":
                    push_to_alias();
                    break;
                case "push_to_topic":
                    push_to_topic();
                    break;
                case "show_command":
                    showNotices();
                    break;

                case "switch_hb_log":
                    showHBLog = !showHBLog;
                    break;
                default:
                    log("Unknown command...",false);
                    break;

            }
            sleep(100);
        }
    }

    private static void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {

        }
    }

    private static void push_to_alias() {
        String alias = input(String.class, "input target alias,separate by ',' if there are some alias");
        String aliasJsonArr = split2JsonArr(alias);

        String msg = input(String.class, "input send msg");
        Integer live_time = input(Integer.class, "input live-time");

        writer2Server(
                String.format(Locale.CHINA,
                        RequestJSON.PUSH_TO_ALIAS,
                        aliasJsonArr,
                        msg,
                        live_time,
                        System.currentTimeMillis()));
    }

    private static void push_to_topic() {
        String topic = input(String.class, "input target topics,separate by ',' if there are some topic");
        String topicJsonArr = split2JsonArr(topic);

        String msg = input(String.class, "input send msg");
        Integer live_time = input(Integer.class, "input live-time");

        writer2Server(
                String.format(Locale.CHINA,
                        RequestJSON.PUSH_TO_TOPICS,
                        topicJsonArr,
                        msg,
                        live_time,
                        System.currentTimeMillis()));
    }

    private static void pull_msg() {
        writer2Server(
                String.format(
                        Locale.CHINA,
                        RequestJSON.PULL_MSG, System.currentTimeMillis()));
    }

    private static void get_topic() {
        writer2Server(
                String.format(
                        Locale.CHINA,
                        RequestJSON.GET_TOPIC, System.currentTimeMillis()));
    }

    private static void register() {
        String trim = input(String.class, "input topic,separate by ',' if there are some topics");
        String topicJsonArr = split2JsonArr(trim);


        String registerJson = String.format(
                Locale.CHINA,
                RequestJSON.REGISTER,
                topicJsonArr, System.currentTimeMillis());
        writer2Server(registerJson);

    }

    private static String split2JsonArr(String trim) {
        trim = trim.trim();
        while (trim.startsWith(",")) {
            trim = trim.substring(1);
            if (trim.length() == 0) return null;
        }

        while (trim.endsWith(",")) {
            trim = trim.substring(0, trim.length() - 1);
            if (trim.length() == 0) return null;
        }
        String[] split = trim.split(",");
        StringBuilder sBuf = new StringBuilder();
        int index = 0;

        for (String str : split) {
            if (index > 0) {
                sBuf.append(",");
            }
            sBuf.append("\"").append(str).append("\"");
            index++;
        }
        return sBuf.toString();
    }

    private static void writer2Server(String msg) {
        try {
            // writer.write(msg+"\n");
            // writer.flush();
            os.write((msg + "\n").getBytes("UTF-8"));
            os.flush();
            log("writer2Server success: " + msg,true);
        } catch (IOException e) {
            log("writer2Server fail: " + msg + ", " + e.toString(),true);

        }
    }

    private static boolean connectServer() {
        Socket socket = new Socket();
        try {
            log("start connect server...",false);
            socket.connect(new InetSocketAddress(SERVER, PORT));

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            os = socket.getOutputStream();
            log("connect server success",false);
            return true;
        } catch (IOException e) {
            log("connect server fail, program finish",false);
        }
        return false;
    }

    public static void showNotices() {
        System.out.println("command: ");
        System.out.println("\tregister," +
                "\n\tget_topic," +
                "\n\tpull_msg," +
                "\n\tpush_to_alias," +
                "\n\tpush_to_topic"+
                "\n\tswitch_hb_log");
    }

    private static <T> T input(Class<T> clz, String hint) {
        System.out.print(hint + ": ");
        while (true) {
            String s = sScanner.nextLine();
            if (clz == Integer.class) {
                try {
                    Integer i = Integer.parseInt(s);
                    return (T) i;
                } catch (Exception e) {
                    System.err.println("invlid number: " + s);
                    continue;
                }
            } else {
                return (T) s;
            }

        }
    }

    private static void log(String msg, boolean flag) {
       /* if (flag)
            System.out.println();*/
        System.out.println(msg);
    }

    private static void eLog(String msg, boolean flag) {
        /*if (flag)
            System.out.println();*/
        System.out.println();
        System.err.println(msg);
    }

    private static boolean readFlag = false;
    private static Runnable readerRunnable = new Runnable() {
        @Override
        public void run() {
            log("start readerRunnable thread",false);
            readFlag = true;
            String line = null;
            Pattern p = Pattern.compile("\"mid\":.*,");
            while (readFlag) {
                try {

                    while ((line = reader.readLine()) != null) {

                        if (line.contains("\"t\":5")) {

                            Matcher m = p.matcher(line);
                            String mid = null;
                            while (m.find()) {
                                String group = m.group();
                                mid = group.replace("\"mid\":", "").replace(",", "").trim();

                            }
                            if (mid != null && mid.matches("[\\d]+"))
                                writer2Server("\"t\":-5,\"mid\":" + mid);
                        }else if(line.contains("\"t\":-1")){
                            if (!showHBLog) {
                                continue;
                            }
                        }
                        eLog("from-server: " + line,true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private static String heartBeatJson = "{\"t\":1,\"alias\":\"%s\"}";
    private static boolean heartBeatFlag = false;
    private static Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            log("start heartbeatrunnable Thread",false);
            heartBeatFlag = true;
            while (heartBeatFlag) {
                String heartbeat = String.format(heartBeatJson, alias);
                // try {
                try {
                    os.write((heartbeat + "\n").getBytes("UTF-8"));
                    if(showHBLog)
                        log("send heartbeat success: " + heartbeat,true);
                } catch (IOException e) {
                    e.printStackTrace();
                    if(showHBLog)
                        log("send heartbeat fail: " + heartbeat,true);
                }

                try {
                    Thread.sleep(frequency * 1000);
                } catch (InterruptedException e) {

                }
            }
        }
    };

    interface RequestJSON {
        String REGISTER = "{\"t\":2,\"topic\":[%s],\"num\":%d}";
        String GET_TOPIC = "{\"t\":3,\"num\":%d}";
        String PULL_MSG = "{\"t\":4,\"num\":%d}";
        String PUSH_TO_ALIAS = "{\"t\":6," +
                "\"method\":\"publish_to_alias\"," +
                "\"alias\":[%s]," +
                "\"msg\":\"%s\"," +
                "\"opts\":{" +
                "\"time_to_live\":%d" +
                "}," +
                "\"num\":%d" + "}";

        String PUSH_TO_TOPICS = "{\"t\":6," +
                "\"method\":\"publish\"," +
                "\"topic\":[%s]," +
                "\"msg\":\"%s\"," +
                "\"opts\":{" +
                "\"time_to_live\":%d" +
                "}," +
                "\"num\":%d" + "}";
    }

}
