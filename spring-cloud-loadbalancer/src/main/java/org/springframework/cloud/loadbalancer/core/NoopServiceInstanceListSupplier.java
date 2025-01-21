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

package org.springframework.cloud.loadbalancer.core;

import java.util.Collections;
import java.util.List;

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;

/**
 * 空返回的{@link ServiceInstanceListSupplier}实现
 *
 *
 * @author Olga Maciaszek-Sharma
 */
public class NoopServiceInstanceListSupplier implements ServiceInstanceListSupplier {

	@Override
	public String getServiceId() {
		// 返回空字符串
		return "";
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		// 返回空集合
		return Flux.defer(() -> Flux.just(Collections.emptyList()));
	}

	@Override
	public Flux<List<ServiceInstance>> get(Request request) {
		// 返回空集合
		return Flux.defer(() -> Flux.just(Collections.emptyList()));
	}

}
