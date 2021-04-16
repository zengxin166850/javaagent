/*
 * @(#)Agent.java	2015-7-24 上午09:49:34
 * javaagent
 * Copyright 2015 wenshuo, Inc. All rights reserved.
 * wenshuo PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.wenshuo.agent;

import com.wenshuo.agent.log.ExecuteLogUtils;
import com.wenshuo.agent.transformer.AgentLogClassFileTransformer;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * Agent
 *
 * @author dingjsh
 */
public class Agent {

    public static void premain(String agentArs, Instrumentation inst) {
        ConfigUtils.initProperties(agentArs);
        ExecuteLogUtils.init();
        inst.addTransformer(new AgentLogClassFileTransformer());
        System.out.println("javaagent start success, premain called!");
    }

    public static void agentmain(String agentArs, Instrumentation inst) {
        // TODO check if javaagent is running，
        System.out.println("agentmain called");
        ConfigUtils.initProperties(agentArs);
        ExecuteLogUtils.init();
        AgentLogClassFileTransformer transformer = new AgentLogClassFileTransformer();
        inst.addTransformer(transformer, true);
        Class[] allLoadedClasses = inst.getAllLoadedClasses();
        //reload
        for (Class clz : allLoadedClasses) {
            try {
                if (transformer.isNeedLogExecuteInfo(clz.getName()))
                    inst.retransformClasses(clz);
            } catch (UnmodifiableClassException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

}
