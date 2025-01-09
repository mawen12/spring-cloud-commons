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

package org.springframework.cloud.client.serviceregistry;

/**
 * 提供服务实例注册注销、状态查询更新的功能
 *
 * @param <R> registration meta data
 * @author Spencer Gibb
 * @since 1.2.0
 */
public interface ServiceRegistry<R extends Registration> {

	/**
	 * 注册一个包含了实例信息的{@link Registration}，
	 * @param registration registration meta data
	 */
	void register(R registration);

	/**
	 * 注销一个{@link Registration}
	 * @param registration registration meta data
	 */
	void deregister(R registration);

	/**
	 * 关闭服务注册，这是一个生命周期函数
	 */
	void close();

	/**
	 * 设置{@link Registration}的状态，需要注意，{@link Registration}本身并未提供设置状态的方法。
	 * Nacos的处理方案是将状态信息发送到Nacos Server上实例的{@code Instance#enabled}中
	 *
	 * @param registration The registration to update.
	 * @param status The status to set.
	 * @see org.springframework.cloud.client.serviceregistry.endpoint.ServiceRegistryEndpoint
	 */
	void setStatus(R registration, String status);

	/**
	 * Gets the status of a particular registration.
	 * 获取特定的{@link Registration}状态。需要注意，{@link Registration}本身并未提供获取状态的方法。
	 * Nacos的处理方案是从Nacos Server上获取实例的{@code Instance#enabled}值
	 *
	 * @param registration The registration to query.
	 * @param <T> The type of the status.
	 * @return The status of the registration.
	 * @see org.springframework.cloud.client.serviceregistry.endpoint.ServiceRegistryEndpoint
	 */
	<T> T getStatus(R registration);

}
