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

public class AdbAutoConnectApplication {
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
        String line = null;
        String[] array = (String[]) null;
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


    public static void main(String[] args) throws Exception {
        String adbPath = "";
        String phoneIP = "";
        String phonePortStart = "35000";
        String phonePortEnd = "50000";
        {
            File configFile = new File("config.properties");
            if (!configFile.exists()) {
                configFile.createNewFile();
                Properties properties = new Properties();
                OutputStream output = null;
                try {
                    output = new FileOutputStream(configFile);
                    properties.setProperty("adbPath", adbPath);
                    properties.setProperty("phoneIP", phoneIP);
                    properties.setProperty("phonePortStart", phonePortStart);
                    properties.setProperty("phonePortEnd", phonePortEnd);
                    // �����ֵ�Ե��ļ���
                    properties.store(output, "�Զ����������ļ�");
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
                    phoneIP = properties.getProperty("phoneIP", phoneIP);
                    phonePortStart = properties.getProperty("phonePortStart", phonePortStart + "");
                    phonePortEnd = properties.getProperty("phonePortEnd", phonePortEnd + "");
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
            boolean exit = false;
            if (adbPath == null || adbPath.trim().length() < 1) {
                System.out.println("������adb.exe����·�� -�� adbPath");
                exit = true;
            }
            if (phoneIP == null || phoneIP.trim().length() < 1) {
                System.out.println("�������ֻ���ip��ַ -�� phoneIP");
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


            if (exit) {
                System.out.println("����100����˳�");
                Thread.sleep(100000);
                System.exit(0);
            }

            System.out.println("adb��ַ��" + adbPath);
            System.out.println("�ֻ�ip��ַ��" + phoneIP);
            System.out.println("ɨ��������ʼ�˿ڣ�" + phonePortStart);
            System.out.println("ɨ�����������˿ڣ�" + phonePortEnd);
        }


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
            System.out.println("����100���߳�����");
            ExecutorService executorService = Executors.newFixedThreadPool(100);
            String ip = phoneIP;
            int port = Integer.parseInt(phonePortStart);
            for (; port < Integer.parseInt(phonePortEnd); port++) {
                int finalPort = port;
                executorService.submit(() -> {
                    if (checkIpPort(ip, finalPort)) {
                        System.out.println("port is open:" + finalPort);
                        try {
                            Runtime mt = Runtime.getRuntime();
                            File myfile = new File("D:\\wanganqing\\Applications\\Android-Sdk\\platform-tools", "adb.exe");
                            System.out.println(myfile.getAbsolutePath() + " connect " + ip + ":" + finalPort);
                            Process process = mt.exec(new String[]{myfile.getAbsolutePath(), "connect", ip + ":" + finalPort});
                            String str;
                            BufferedReader buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            while ((str = (buffer.readLine())) != null) {
                                System.out.println(str);
                                if (str.contains(ip) && str.contains("connected")) {
                                    System.out.println("�˳�");
                                    System.exit(0);
                                }
                            }
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                });
            }
        }
    }
}
