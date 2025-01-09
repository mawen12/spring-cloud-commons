/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.loadbalancer;

import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;

/**
 * 负载均衡重试工厂，用于构造请求失败的重试行为
 *
 * @author Ryan Baxter
 */
public interface LoadBalancedRetryFactory {

	/**
	 * 创建特定服务的负载均衡重试策略，默认返回空策略
	 *
	 * @param service The ID of the service to create the retry policy for.
	 * @param serviceInstanceChooser Used to get the next server from a load balancer.
	 * @return A retry policy for the service.
	 */
	default LoadBalancedRetryPolicy createRetryPolicy(String service, ServiceInstanceChooser serviceInstanceChooser) {
		return null;
	}

	/**
	 * 创建特定服务的重试监听器数组，默认返回空数组
	 *
	 * @param service The service to create the {@link RetryListener}s for.
	 * @return An array of {@link RetryListener}s.
	 */
	default RetryListener[] createRetryListeners(String service) {
		return new RetryListener[0];
	}

	/**
	 * 创建特定服务的回退策略，默认为无退避策略
	 *
	 * @param service The service to create the {@link BackOffPolicy} for.
	 * @return The {@link BackOffPolicy}.
	 */
	default BackOffPolicy createBackOffPolicy(String service) {
		return new NoBackOffPolicy();
	}

}
