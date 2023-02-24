import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class AdbAutoConnectApplication {


    public static void main(String[] args) throws Exception {
        String adbPath = "";
        String phoneIP = "192.168.1.94";
        String[] phoneIPs = new String[]{phoneIP};
        String phonePortStart = "35000";
        String phonePortEnd = "50000";
        String threadNum = "100";
        ThreadPoolExecutor executorService;

        {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            System.out.println(name);
            String cpid = name.split("@")[0];
            System.out.println("��ǰ����pid:" + cpid);
            String pidName = getPidNameByPid(cpid).replaceAll(".exe", "");
            System.out.println("��ǰ����name:" + pidName);
            //��ȡ��ǰ�����PID
            List<String> list = getPIDListByPidName(pidName);
            list.remove(cpid);
            for (String pid : list) {
                //ֹͣɱ��pid
                System.out.println("ֹͣɱ��֮ǰ�����ĳ���" + pid);
                String[] cmd = {"cmd.exe", "/c", "taskkill -f /pid " + pid};
                Runtime.getRuntime().exec(cmd, null, new File(new File("").getCanonicalPath())).waitFor();
            }
        }

        {
            File configFile = new File("config.properties");
            if (!configFile.exists()) {
                configFile.createNewFile();
                Properties properties = new Properties();
                OutputStream output = null;
                try {
                    output = new FileOutputStream(configFile);
                    properties.setProperty("adbPath", adbPath);
                    properties.setProperty("phoneIP", array2String(phoneIPs));
                    properties.setProperty("phonePortStart", phonePortStart);
                    properties.setProperty("phonePortEnd", phonePortEnd);
                    properties.setProperty("threadNum", threadNum);
                    properties.store(output, "�Զ����������ļ���ip��ַ�ö��Ÿ���");
                    System.out.println("�Զ����������ļ�," + configFile.getAbsolutePath());
                } catch (IOException io) {
                    io.printStackTrace();
                } finally {
                    if (output != null) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            } else {
                Properties properties = new Properties();
                InputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(configFile);
                    properties.load(inputStream);
                    adbPath = properties.getProperty("adbPath", adbPath);
                    phoneIPs = string2Array(properties.getProperty("phoneIP", array2String(phoneIPs)));
                    phonePortStart = properties.getProperty("phonePortStart", phonePortStart);
                    phonePortEnd = properties.getProperty("phonePortEnd", phonePortEnd);
                    threadNum = properties.getProperty("threadNum", threadNum);
                    System.out.println("��ȡ�����ļ�," + configFile.getAbsolutePath());
                } catch (IOException io) {
                    io.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }


        {


            boolean exit = false;
            if (adbPath == null || adbPath.trim().length() < 1) {
                System.out.println("������adb.exe����·�� -�� adbPath");
                exit = true;
            }

            for (String ip : phoneIPs)
                if (ip == null || ip.trim().length() < 1) {
                    System.out.println("�������ֻ���ip��ַ -�� phoneIPs ��ʽ���ԣ�" + ip);
                    System.out.println("�������ֻ���ip��ַ -�� phoneIPs ��ʽ�ǣ�127.0.0.l , 127.0.0.l , 127.0.0.l ");
                    exit = true;
                }

            if (phonePortStart == null || phonePortStart.trim().length() < 1) {
                System.out.println("������ɨ��Ŀ�ʼ�˿ں� -�� phonePortStart");
                exit = true;
            } else {
                try {
                    Integer.parseInt(phonePortStart);
                } catch (Exception e) {
                    System.out.println("������ɨ��Ŀ�ʼ�˿ں� Ϊ���� -�� phonePortStart");
                }
            }
            if (phonePortEnd == null || phonePortEnd.trim().length() < 1) {
                System.out.println("������ɨ��Ľ����˿ں� -�� phonePortEnd");
                exit = true;
            } else {
                try {
                    Integer.parseInt(phonePortEnd);
                } catch (Exception e) {
                    System.out.println("������ɨ��Ľ����˿ں� Ϊ���� -�� phonePortEnd");
                }
            }

            if (threadNum == null || threadNum.trim().length() < 1) {
                System.out.println("������ɨ���߳��� -�� threadNum");
                exit = true;
            } else {
                try {
                    Integer.parseInt(threadNum);
                } catch (Exception e) {
                    System.out.println("������ɨ��Ľ����˿ں� Ϊ���� -�� threadNum");
                }
            }


            if (exit) {
                System.out.println("����100����˳�");
                Thread.sleep(100000);
                System.exit(0);
            }

            System.out.println("adb��ַ��" + adbPath);
            System.out.println("�ֻ�ip��ַ�б�" + array2String(phoneIPs));
            System.out.println("ɨ��������ʼ�˿ڣ�" + phonePortStart);
            System.out.println("ɨ�����������˿ڣ�" + phonePortEnd);
            System.out.println("ɨ���߳�����" + threadNum);
        }



        {

            executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(Integer.parseInt(threadNum));
            for (String ip : phoneIPs) {
                if (!checkIp(ip)) {
                    System.out.println("�޷����ӣ��ֻ�ip��ַ��" + ip);
                    continue;
                }
                System.out.println("���ɨ���ֻ�ip��ַ��" + ip);
                int port = Integer.parseInt(phonePortStart);
                for (; port < Integer.parseInt(phonePortEnd); port++) {
                    int finalPort = port;
                    String finalAdbPath = adbPath;
                    executorService.submit(() -> {
                        if (checkIpPort(ip, finalPort)) {
                            System.out.println("port is open:" + finalPort);
                            try {
                                Runtime mt = Runtime.getRuntime();
                                File myfile = new File(finalAdbPath);
                                System.out.println(myfile.getAbsolutePath() + " connect " + ip + ":" + finalPort);
                                Process process = mt.exec(new String[]{myfile.getAbsolutePath(), "connect", ip + ":" + finalPort});
                                String str;
                                BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
                                while ((str = (buffer.readLine())) != null) {
                                    System.out.println(str);
                                    if (str.contains(ip) && str.contains("connected")) {
                                        System.out.println("���ӳɹ� ip:" + ip + " port:" + finalPort);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

            }
        }

        {
            Thread.sleep(100);
            System.out.println("ɨ����������" + executorService.getTaskCount());
            while (executorService.getActiveCount() > 0) {
                System.out.println("ʣ��ɨ������" + (executorService.getTaskCount() - executorService.getCompletedTaskCount()));
                Thread.sleep(1000);
            }
            System.out.println("ʣ��ɨ������" + (executorService.getTaskCount() - executorService.getCompletedTaskCount()));

            System.out.print("ɨ����� ");
            for (int i = 0; i < 10; i++) {
                Thread.sleep(500);
                System.out.print(".");
            }
            executorService.shutdownNow();
        }
    }

    // ͨ��Pid��ȡPidName
    public static String getPidNameByPid(String pid) throws Exception {
        String pidName = null;
        InputStream is = null;
        InputStreamReader ir = null;
        BufferedReader br = null;
        String line = null;
        String[] array = (String[]) null;
        try {
            Process p = Runtime.getRuntime().exec("TASKLIST /NH /FO CSV /FI \"PID EQ " + pid + "\"");
            is = p.getInputStream(); // "javaw.exe","3856","Console","1","72,292
            // K"����������л�ȡ��Ӧ��PidName
            ir = new InputStreamReader(is);
            br = new BufferedReader(ir);
            while ((line = br.readLine()) != null) {
                if (line.indexOf(pid) != -1) {
                    array = line.split(",");
                    line = array[0].replaceAll("\"", "");
                    line = line.replaceAll(".exe", "");// ����pidName��׺Ϊexe����EXE
                    line = line.replaceAll(".exe".toUpperCase(), "");
                    pidName = line;
                }
            }
        } catch (IOException localIOException) {
            throw new Exception("��ȡ�������Ƴ���");
        } finally {
            if (br != null) {
                br.close();
            }
            if (ir != null) {
                ir.close();
            }
            if (is != null) {
                is.close();
            }
        }
        return pidName;
    }


    public static List<String> getPIDListByPidName(String pidName) throws Exception {
        List<String> pidList = new ArrayList<>();
        InputStream is = null;
        InputStreamReader ir = null;
        BufferedReader br = null;
        String line;
        String[] array;
        try {
            String imageName = pidName + ".exe";
            Process p = Runtime.getRuntime().exec("TASKLIST /NH /FO CSV /FI \"IMAGENAME EQ " + imageName + "\"");
            is = p.getInputStream();
            ir = new InputStreamReader(is);
            br = new BufferedReader(ir);
            while ((line = br.readLine()) != null) {
                if (line.indexOf(imageName) != -1) {
                    array = line.split(",");
                    line = array[1].replaceAll("\"", "");
                    pidList.add(line);
                }
            }
        } catch (IOException localIOException) {
            throw new Exception("��ȡ����ID����");
        } finally {
            if (br != null) {
                br.close();
            }
            if (ir != null) {
                ir.close();
            }
            if (is != null) {
                is.close();
            }
        }
        return pidList;
    }


    /**
     * ���Ip�Ͷ˿��Ƿ����
     *
     * @param ip
     * @param port
     * @return
     */
    public static boolean checkIpPort(String ip, int port) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(ip, port), 300);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {

                }
            }
        }

    }

    /**
     * ���Ip��ַ
     *
     * @param ip
     * @return
     */
    public static boolean checkIp(String ip) {
        try {
            InetAddress.getByName(ip).isReachable(300);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String[] string2Array(String phoneIPs) {
        if (phoneIPs == null || phoneIPs.trim().length() == 0) {
            return new String[]{"192.168.1.94"};

        }
        phoneIPs = phoneIPs.trim();
        phoneIPs = phoneIPs.replaceAll("��", ",");
        phoneIPs = phoneIPs.replaceAll("��", ",");
        phoneIPs = phoneIPs.replaceAll(";", ",");
        phoneIPs = phoneIPs.replaceAll(" ", "");
        return phoneIPs.split(",");
    }

    private static String array2String(String[] phoneIPs) {
        if (phoneIPs == null) {
            return "";
        }
        String s = "";
        for (String ip : phoneIPs)
            s += ip + ",";

        return s;
    }
}
