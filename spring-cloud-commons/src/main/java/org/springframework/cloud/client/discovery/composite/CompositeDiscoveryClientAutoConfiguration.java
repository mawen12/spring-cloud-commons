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

package org.springframework.cloud.client.discovery.composite;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 用于复合的服务发现客户端的自动配置
 * <p>
 * 其执行顺序为：{@link SimpleDiscoveryClientAutoConfiguration} -> This
 * 这是因为复合的客户端中之一有可能是{@link org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient}
 *
 * @author Biju Kunjummen
 */

@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(SimpleDiscoveryClientAutoConfiguration.class)
public class CompositeDiscoveryClientAutoConfiguration {

	@Bean
	@Primary
	public CompositeDiscoveryClient compositeDiscoveryClient(List<DiscoveryClient> discoveryClients) {
		return new CompositeDiscoveryClient(discoveryClients);
	}

}
