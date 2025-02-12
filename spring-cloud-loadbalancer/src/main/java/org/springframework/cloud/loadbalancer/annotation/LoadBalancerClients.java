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

package org.springframework.cloud.loadbalancer.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 允许用户在一个类上组合多个{@link LoadBalancerClient}注解的遍历注解。
 *
 * @author Dave Syer
 * @see LoadBalancerClient
 * @see LoadBalancerClientConfigurationRegistrar
 */
@Configuration(proxyBeanMethods = false)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import(LoadBalancerClientConfigurationRegistrar.class)
public @interface LoadBalancerClients {

	LoadBalancerClient[] value() default {};

	/**
	 * {@link LoadBalancerClientConfigurationRegistrar}创建{@link LoadBalancerClientSpecification}作为参数。
	 * 这些依次被添加为{@link LoadBalancerClientFactory}中的默认上下文。
	 * 如果未通过{@link LoadBalancerClient#configuration()}定义值，则这些类中定义的配置将用作默认值。
	 *
	 * @return classes for default configurations
	 */
	Class<?>[] defaultConfiguration() default {};

}
