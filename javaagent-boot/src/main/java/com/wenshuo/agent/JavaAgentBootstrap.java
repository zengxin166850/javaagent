/**
 * @projectName javaagent
 * @package main.java.agent
 * @className com.wenshuo.agent.Test
 * @copyright Copyright 2021 Thuisoft, Inc. All rights reserved.
 */
package com.wenshuo.agent;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.wenshuo.agent.util.JavaVersionUtils;

import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Test
 *
 * @author zengxin
 * @version 1.0
 * @date 2021/4/15 15:37
 */
public class JavaAgentBootstrap {

    private static final String DEFAULT_AGENT_JAR = "/javaagent-core-2.1.0.jar";
    private static final String AGENT_PATH = "agentpath";

    public static void main(String[] args) throws Exception {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String currentPid = name.split("@")[0];

        List<VirtualMachineDescriptor> existsJavaProcessList = new ArrayList<VirtualMachineDescriptor>();

        //https://stackoverflow.com/questions/57006881
        Class.forName("sun.tools.attach.WindowsAttachProvider");
        int count = 1;
        System.out.println("Found existing java process, please choose one and input the serial number of the process, eg : 1. Then hit ENTER.");
        for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
            if (descriptor.id().equals(currentPid)) {
                continue;
            } else if (count == 1) {
                System.out.println("* [" + count + "]: " + descriptor.displayName());
            } else {
                System.out.println("  [" + count + "]: " + descriptor.id() + " " + descriptor.displayName());
            }
            existsJavaProcessList.add(descriptor);
            count++;
        }

        VirtualMachineDescriptor descriptor = null;
        try {
            int choice = new Scanner(System.in).nextInt();
            if (choice < 0 || choice > existsJavaProcessList.size()) {
                System.out.println("Please select an available pid.");
                System.exit(1);
            }
            descriptor = existsJavaProcessList.get(choice - 1);
        } catch (InputMismatchException e) {
            System.out.println("Please input an integer to select pid.");
            System.exit(1);
        }
        //
        JavaAgentBootstrap.attachAgent(descriptor);
    }

    public static void attachAgent(VirtualMachineDescriptor descriptor) throws Exception {
        VirtualMachine virtualMachine = null;
        try {
            virtualMachine = VirtualMachine.attach(descriptor);

            Properties targetSystemProperties = virtualMachine.getSystemProperties();
            String targetJavaVersion = JavaVersionUtils.javaVersionStr(targetSystemProperties);
            String currentJavaVersion = JavaVersionUtils.javaVersionStr();
            if (targetJavaVersion != null && currentJavaVersion != null) {
                if (!targetJavaVersion.equals(currentJavaVersion)) {
                    String javaVersionWarn = "[\033[33mWARN\033[0m] Current VM java version: %s do not match target " +
                            "VM java version: %s, attach may fail.";
                    System.out.println(String.format(javaVersionWarn, currentJavaVersion, targetJavaVersion));
                    String javaHomeWarn = "[\033[33mWARN\033[0m] Target VM JAVA_HOME is %s, arthas-boot JAVA_HOME is %s, " +
                            "try to set the same JAVA_HOME.";
                    System.out.println(String.format(javaHomeWarn, targetSystemProperties.getProperty("java.home"),
                            System.getProperty("java.home")));
                }
            }
            // get the agent jar location
            String pwd = JavaAgentBootstrap.class.getProtectionDomain().getCodeSource().getLocation().getFile();
            String jarPath = pwd.substring(1, pwd.lastIndexOf("/"));
            String agentpath = System.getProperty(AGENT_PATH, jarPath + DEFAULT_AGENT_JAR);
            System.out.println("current agentpath: " + agentpath);
            virtualMachine.loadAgent(agentpath);
            System.out.println(String.format("attach %s success", descriptor.id()));
        } finally {
            //
            if (null != virtualMachine) {
                virtualMachine.detach();
            }
        }
    }

}
