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

package org.springframework.cloud.client;

import java.net.URI;
import java.util.Map;

/**
 * 代表服务发现系统中的一个服务实例
 *
 * @author Spencer Gibb
 * @author Tim Ysewyn
 */
public interface ServiceInstance {

	/**
	 * @return 返回实例ID，每个实例都是唯一的
	 */
	default String getInstanceId() {
		return null;
	}

	/**
	 * @return 返回服务ID
	 */
	String getServiceId();

	/**
	 * @return 注册的服务实例的主机名
	 */
	String getHost();

	/**
	 * @return 注册的服务实例的端口
	 */
	int getPort();

	/**
	 * @return 注册的服务实例的端口是否使用HTTPS
	 */
	boolean isSecure();

	/**
	 * @return 服务的URI地址
	 */
	URI getUri();

	/**
	 * @return 与服务实例相关联的元数据键值对
	 */
	Map<String, String> getMetadata();

	/**
	 * @return 服务实例的scheme，即端口之前的内容
	 */
	default String getScheme() {
		return null;
	}

}
