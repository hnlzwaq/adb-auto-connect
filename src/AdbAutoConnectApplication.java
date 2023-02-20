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
    // 通过Pid获取PidName
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
            // K"从这个进程中获取对应的PidName
            ir = new InputStreamReader(is);
            br = new BufferedReader(ir);
            while ((line = br.readLine()) != null) {
                if (line.indexOf(pid) != -1) {
                    array = line.split(",");
                    line = array[0].replaceAll("\"", "");
                    line = line.replaceAll(".exe", "");// 考虑pidName后缀为exe或者EXE
                    line = line.replaceAll(".exe".toUpperCase(), "");
                    pidName = line;
                }
            }
        } catch (IOException localIOException) {
            throw new Exception("获取进程名称出错！");
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
            throw new Exception("获取进程ID出错！");
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
     * 检测Ip和端口是否可用
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
     * 检测Ip地址
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
        String threadNum = "100";

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
                    properties.setProperty("threadNum", threadNum);
                    properties.store(output, "自动生成配置文件");
                    System.out.println("自动生成配置文件," + configFile.getAbsolutePath());
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
                    phonePortStart = properties.getProperty("phonePortStart", phonePortStart);
                    phonePortEnd = properties.getProperty("phonePortEnd", phonePortEnd);
                    threadNum = properties.getProperty("threadNum", threadNum);
                    System.out.println("读取配置文件," + configFile.getAbsolutePath());
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
                System.out.println("请设置adb.exe绝对路径 -》 adbPath");
                exit = true;
            }
            if (phoneIP == null || phoneIP.trim().length() < 1) {
                System.out.println("请设置手机的ip地址 -》 phoneIP");
                exit = true;
            }
            if (phonePortStart == null || phonePortStart.trim().length() < 1) {
                System.out.println("请设置扫描的开始端口号 -》 phonePortStart");
                exit = true;
            } else {
                try {
                    Integer.parseInt(phonePortStart);
                } catch (Exception e) {
                    System.out.println("请设置扫描的开始端口号 为数字 -》 phonePortStart");
                }
            }
            if (phonePortEnd == null || phonePortEnd.trim().length() < 1) {
                System.out.println("请设置扫描的结束端口号 -》 phonePortEnd");
                exit = true;
            } else {
                try {
                    Integer.parseInt(phonePortEnd);
                } catch (Exception e) {
                    System.out.println("请设置扫描的结束端口号 为数字 -》 phonePortEnd");
                }
            }

            if (threadNum == null || threadNum.trim().length() < 1) {
                System.out.println("请设置扫描线程数 -》 threadNum");
                exit = true;
            } else {
                try {
                    Integer.parseInt(threadNum);
                } catch (Exception e) {
                    System.out.println("请设置扫描的结束端口号 为数字 -》 threadNum");
                }
            }


            if (exit) {
                System.out.println("程序100秒后退出");
                Thread.sleep(100000);
                System.exit(0);
            }

            System.out.println("adb地址：" + adbPath);
            System.out.println("手机ip地址：" + phoneIP);
            System.out.println("扫描启动开始端口：" + phonePortStart);
            System.out.println("扫描启动结束端口：" + phonePortEnd);
            System.out.println("扫描线程数：" + threadNum);
        }


        {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            System.out.println(name);
            String cpid = name.split("@")[0];
            System.out.println("当前进程pid:" + cpid);
            String pidName = getPidNameByPid(cpid).replaceAll(".exe", "");
            System.out.println("当前进程name:" + pidName);
            //获取当前程序的PID
            List<String> list = getPIDListByPidName(pidName);
            list.remove(cpid);
            for (String pid : list) {
                //停止杀死pid
                System.out.println("停止杀死之前启动的程序：" + pid);
                String[] cmd = {"cmd.exe", "/c", "taskkill -f /pid " + pid};
                Runtime.getRuntime().exec(cmd, null, new File(new File("").getCanonicalPath())).waitFor();
            }
        }

        {
            System.out.println("启动" + threadNum + "个线程链接");
            ExecutorService executorService = Executors.newFixedThreadPool(Integer.parseInt(threadNum));
            String ip = phoneIP;
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
                                    System.out.println("退出");
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
