/*
 * Copyright 2012-2023 the original author or authors.
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

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpRequest;

/**
 * 负载均衡请求转换器，允许使用给定的{@link ServiceInstance}来转换负载均衡的{@link HttpRequest}
 *
 * @author Will Tran
 */
@Order(LoadBalancerRequestTransformer.DEFAULT_ORDER)
public interface LoadBalancerRequestTransformer {

	/**
	 * Order for the load balancer request transformer.
	 */
	int DEFAULT_ORDER = 0;

	HttpRequest transformRequest(HttpRequest request, ServiceInstance instance);

}
