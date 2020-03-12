/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.jvm;

import org.ballerinalang.jvm.observability.ObservabilityConstants;
import org.ballerinalang.jvm.observability.ObserveUtils;
import org.ballerinalang.jvm.observability.ObserverContext;
import org.ballerinalang.jvm.scheduling.Scheduler;
import org.ballerinalang.jvm.scheduling.State;
import org.ballerinalang.jvm.scheduling.Strand;
import org.ballerinalang.jvm.types.BTypes;
import org.ballerinalang.jvm.values.ErrorValue;
import org.ballerinalang.jvm.values.FutureValue;
import org.ballerinalang.jvm.values.ObjectValue;
import org.ballerinalang.jvm.values.connector.CallableUnitCallback;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * External API to be used by the interop users to control Ballerina runtime behavior.
 *
 * @since 1.0.0
 */
public class BRuntime {

    private Scheduler scheduler;

    private BRuntime(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public static CompletableFuture<Object> markAsync() {
        Strand strand = Scheduler.getStrand();
        strand.blockedOnExtern = true;
        strand.setState(State.BLOCK_AND_YIELD);
        CompletableFuture<Object> future = new CompletableFuture<>();
        future.whenComplete(new Unblocker(strand));
        return future;
    }

    public static BRuntime getCurrentRuntime() {
        Strand strand = Scheduler.getStrand();
        return new BRuntime(strand.scheduler);
    }

    public void invokeMethodAsync(ObjectValue object, String methodName, Object... args) {
        Function<?, ?> func = o -> object.call((Strand) (((Object[]) o)[0]), methodName, args);
        scheduler.schedule(new Object[1], func, null, null);
    }

    public void invokeMethodAsync(ObjectValue object, String methodName,
                                  CallableUnitCallback callback, Object... args) {
        Function<?, ?> func = o -> object.call((Strand) (((Object[]) o)[0]), methodName, args);
        scheduler.schedule(new Object[1], func, null, callback);
    }

    public void invokeMethodAsync(ObjectValue object, String methodName,
                                  CallableUnitCallback callback, Map<String, Object> properties, Object... args) {
        Function<Object[], Object> func = objects -> {
            Strand strand = (Strand) objects[0];
            if (ObserveUtils.isObservabilityEnabled() && properties != null &&
                    properties.containsKey(ObservabilityConstants.KEY_OBSERVER_CONTEXT)) {
                strand.observerContext =
                        (ObserverContext) properties.remove(ObservabilityConstants.KEY_OBSERVER_CONTEXT);
            }
            return object.call(strand, methodName, args);
        };
        scheduler.schedule(new Object[1], func, null, callback, properties, BTypes.typeNull);
    }

    public void invokeMethodSync(ObjectValue object, String methodName, Object... args) {
        Function<?, ?> func = o -> object.call((Strand) (((Object[]) o)[0]), methodName, args);
        Semaphore semaphore = new Semaphore(0);
        final ErrorValue[] errorValue = new ErrorValue[1];
        scheduler.schedule(new Object[1], func, null, new CallableUnitCallback() {
            @Override
            public void notifySuccess() {
                semaphore.release();
            }

            @Override
            public void notifyFailure(ErrorValue error) {
                errorValue[0] = error;
                semaphore.release();
            }
        });
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            // Ignore
        }
        if (errorValue[0] != null) {
            throw errorValue[0];
        }
    }

    /**
     * Invoke Ballerina function and get the result.
     *
     * @param object     Ballerina object in which the function is defined.
     * @param methodName Ballerina function name, to invoke.
     * @param timeout    Timeout in milliseconds to wait until acquiring the semaphore.
     * @param args       Ballerina function arguments.
     * @return Ballerina function invoke result.
     */
    public Object getSyncMethodInvokeResult(ObjectValue object, String methodName, int timeout, Object... args) {
        Function<?, ?> func = o -> object.call((Strand) (((Object[]) o)[0]), methodName, args);
        Semaphore semaphore = new Semaphore(0);
        final ErrorValue[] errorValue = new ErrorValue[1];
        // Add 1 more element to keep null for add the strand later.
        Object[] params = new Object[]{null, args};
        FutureValue futureValue = scheduler.schedule(params, func, null, new CallableUnitCallback() {
            @Override
            public void notifySuccess() {
                semaphore.release();
            }

            @Override
            public void notifyFailure(ErrorValue error) {
                errorValue[0] = error;
                semaphore.release();
            }
        });
        try {
            semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // Ignore
        }
        if (errorValue[0] != null) {
            throw errorValue[0];
        }
        return futureValue.result;
    }

    private static class Unblocker implements java.util.function.BiConsumer<Object, Throwable> {

        private Strand strand;

        public Unblocker(Strand strand) {
            this.strand = strand;
        }

        @Override
        public void accept(Object returnValue, Throwable throwable) {
            if (throwable == null) {
                this.strand.setReturnValues(returnValue);
                this.strand.scheduler.unblockStrand(strand);
            }
        }
    }

}
