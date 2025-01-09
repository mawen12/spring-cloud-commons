/*
 * Copyright 2013-2024 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 用于Spring Cloud负载均衡器客户端的配置属性，即基于客户端的自定义配置
 * 各个客户端通过{@link #clients}进行单独配置，默认值和其他属性位于{@link LoadBalancerProperties}中。
 *
 * @author Spencer Gibb
 * @since 3.1.0
 */
@ConfigurationProperties("spring.cloud.loadbalancer")
public class LoadBalancerClientsProperties extends LoadBalancerProperties {

	/**
	 * Map<客户端标识, 对应客户端的负载均衡属性配置>
	 */
	private final Map<String, LoadBalancerProperties> clients = new HashMap<>();

	public Map<String, LoadBalancerProperties> getClients() {
		return this.clients;
	}

}
