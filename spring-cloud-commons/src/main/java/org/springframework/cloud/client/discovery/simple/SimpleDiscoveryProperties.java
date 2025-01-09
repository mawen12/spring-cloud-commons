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

package org.springframework.cloud.client.discovery.simple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * Spring Cloud简单服务发现属性，用于构造服务实例，其读取 PROPERTIES(spring.cloud.discovery.client.simple)
 * <p>
 * 其中包含用户可配置的顺序，该顺序将用于在{@link org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient}
 * 使用的客户端列表中确定此客户端的优先级，或者成为权重。
 * <p>
 * 该类继承了{@link InitializingBean}其会在初始化完之后回调
 *
 * @author Biju Kunjummen
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 * @author Charu Covindane
 */

@ConfigurationProperties(prefix = "spring.cloud.discovery.client.simple")
public class SimpleDiscoveryProperties implements InitializingBean {

	/**
	 * Map<服务Id, 该服务下的所有实例>
	 */
	private Map<String, List<DefaultServiceInstance>> instances = new HashMap<>();

	/**
	 * The properties of the local instance (if it exists). Users should set these
	 * properties explicitly if they are exporting data (e.g. metrics) that need to be
	 * identified by the service instance.
	 */
	@NestedConfigurationProperty
	private DefaultServiceInstance local = new DefaultServiceInstance(null, null, null, 0, false);

	/**
	 * 顺序，也可是为优先级
	 */
	private int order = DiscoveryClient.DEFAULT_ORDER;

	public Map<String, List<DefaultServiceInstance>> getInstances() {
		return this.instances;
	}

	public void setInstances(Map<String, List<DefaultServiceInstance>> instances) {
		this.instances = instances;
	}

	public DefaultServiceInstance getLocal() {
		return this.local;
	}

	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public void afterPropertiesSet() {
		for (String key : this.instances.keySet()) {
			/**
			 * 将key作为实例的服务ID
			 */
			for (DefaultServiceInstance instance : this.instances.get(key)) {
				instance.setServiceId(key);
			}
		}
	}

	public void setInstance(String serviceId, String host, int port) {
		local = new DefaultServiceInstance(null, serviceId, host, port, false);
	}

}
