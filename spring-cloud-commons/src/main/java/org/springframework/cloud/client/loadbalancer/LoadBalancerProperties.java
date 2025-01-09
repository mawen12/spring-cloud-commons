/*
 * Copyright 2012-2024 the original author or authors.
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

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import reactor.util.retry.RetryBackoffSpec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.commons.util.IdUtils;
import org.springframework.core.env.PropertyResolver;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.client.RestTemplate;

/**
 * 用于Spring Cloud负载均衡器的基本配置，即通用配置
 *
 * @see LoadBalancerClientsProperties
 * @author Olga Maciaszek-Sharma
 * @author Gandhimathi Velusamy
 * @author Zhuozhi Ji
 * @since 2.2.1
 */
public class LoadBalancerProperties {

	/**
	 * 健康检查属性，包含健康检查的调用行为、配置信息等
	 */
	private HealthCheck healthCheck = new HealthCheck();

	/**
	 * 影响负载均衡亲的请求以及随后被用于{@link ReactiveLoadBalancer}的实现
	 */
	private Map<String, String> hint = new LinkedCaseInsensitiveMap<>();

	/**
	 * hint的请求头名称，用于在过滤时，传递hit给基于hit的实例
	 */
	private String hintHeaderName = "X-SC-LB-Hint";

	/**
	 * 用于设置Spring Cloud中负载均衡器下Spring-Retry和Reactor Retry的行为
	 */
	private Retry retry = new Retry();

	/**
	 * 用于配置粘连会话的负载均衡器实现
	 */
	private StickySession stickySession = new StickySession();

	/**
	 * If this flag is set to {@code true},
	 * {@code ServiceInstanceListSupplier#get(Request request)} method will be implemented
	 * to call {@code delegate.get(request)} in classes assignable from
	 * {@code DelegatingServiceInstanceListSupplier} that don't already implement that
	 * method, with the exclusion of {@code CachingServiceInstanceListSupplier} and
	 * {@code HealthCheckServiceInstanceListSupplier}, which should be placed in the
	 * instance supplier hierarchy directly after the supplier performing instance
	 * retrieval over the network, before any request-based filtering is done,
	 * {@code true} by default.
	 */
	private boolean callGetWithRequestOnDelegates = true;

	/**
	 * Properties for
	 * {@code org.springframework.cloud.loadbalancer.core.SubsetServiceInstanceListSupplier}.
	 */
	private Subset subset = new Subset();

	/**
	 * X-Forwarded主机和协议头信息
	 */
	private XForwarded xForwarded = new XForwarded();

	/**
	 * 负载均衡指标相关属性
	 */
	private Stats stats = new Stats();

	public HealthCheck getHealthCheck() {
		return healthCheck;
	}

	public void setHealthCheck(HealthCheck healthCheck) {
		this.healthCheck = healthCheck;
	}

	public Map<String, String> getHint() {
		return hint;
	}

	public void setHint(Map<String, String> hint) {
		this.hint = hint;
	}

	public Retry getRetry() {
		return retry;
	}

	public void setRetry(Retry retry) {
		this.retry = retry;
	}

	public StickySession getStickySession() {
		return stickySession;
	}

	public void setStickySession(StickySession stickySession) {
		this.stickySession = stickySession;
	}

	public String getHintHeaderName() {
		return hintHeaderName;
	}

	public void setHintHeaderName(String hintHeaderName) {
		this.hintHeaderName = hintHeaderName;
	}

	// TODO: fix spelling in a major release
	public void setxForwarded(XForwarded xForwarded) {
		this.xForwarded = xForwarded;
	}

	public XForwarded getXForwarded() {
		return xForwarded;
	}

	public boolean isCallGetWithRequestOnDelegates() {
		return callGetWithRequestOnDelegates;
	}

	public Subset getSubset() {
		return subset;
	}

	public void setSubset(Subset subset) {
		this.subset = subset;
	}

	public void setCallGetWithRequestOnDelegates(boolean callGetWithRequestOnDelegates) {
		this.callGetWithRequestOnDelegates = callGetWithRequestOnDelegates;
	}

	public Stats getStats() {
		return stats;
	}

	public void setStats(Stats stats) {
		this.stats = stats;
	}

	/**
	 * 粘连会话，主要用于设置会话是否传递
	 */
	public static class StickySession {

		/**
		 * Cookie中保存首选实例ID的名称
		 */
		private String instanceIdCookieName = "sc-lb-instance-id";

		/**
		 * 指示Spring Cloud负载均衡器是否应添加带有新选定实例的cookie
		 */
		private boolean addServiceInstanceCookie = false;

		public String getInstanceIdCookieName() {
			return instanceIdCookieName;
		}

		public void setInstanceIdCookieName(String instanceIdCookieName) {
			this.instanceIdCookieName = instanceIdCookieName;
		}

		public boolean isAddServiceInstanceCookie() {
			return addServiceInstanceCookie;
		}

		public void setAddServiceInstanceCookie(boolean addServiceInstanceCookie) {
			this.addServiceInstanceCookie = addServiceInstanceCookie;
		}

	}

	/**
	 * XForwarded开关，如果打开了，则会在{@link org.springframework.cloud.loadbalancer.blocking.XForwardedHeadersTransformer}
	 * 中设置{@code X-Forwarded-Host}和{@code X-Forwarded-Proto}请求头
	 */
	public static class XForwarded {

		/**
		 * 是否启用X-Forwarded头
		 */
		private boolean enabled = false;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	/**
	 * 健康检查，设置健康检查的执行间隔、执行目标等
	 */
	public static class HealthCheck {

		/**
		 * 健康检查调度的初始延迟，默认为不延迟
		 */
		private Duration initialDelay = Duration.ZERO;

		/**
		 * 健康检查调度的执行执行间隔，默认为25s
		 */
		private Duration interval = Duration.ofSeconds(25);

		/**
		 * 重新拉取可用的服务实例的执行间隔，默认25s
		 */
		private Duration refetchInstancesInterval = Duration.ofSeconds(25);

		/**
		 * 健康检查请求的目标路径，可根据服务Id设置，也可使用默认值，如果没有设置，则采用DEFAULT(/actuator/health)
		 */
		private Map<String, String> path = new LinkedCaseInsensitiveMap<>();

		/**
		 * 健康检查请求的端口，默认为服务实例上所请求服务可用的端口
		 */
		private Integer port;

		/**
		 * 指示{@code HealthCheckServiceInstanceListSupplier}是否应该重新拉取实例。默认为false
		 * 如果实例信息可以更新，并且底层委托不提供持续的变化，则可以使用此方法。
		 */
		private boolean refetchInstances = false;

		/**
		 * 指示健康见擦汗是否应不断重复。如果有其他地方定期获取实例，则将其设置为false可能会很有用。
		 * 因为每次重新获取也会触发健康检查。默认为true。
		 * 像是Nacos就提供了服务端更新时，推送到客户端的功能，这就代表可以将该值设置为false。
		 */
		private boolean repeatHealthCheck = true;

		/**
		 * 指示是否应该在已检索到的每个活动的{@link ServiceInstance}上发出{@code healthCheckFlux}。
		 * 如果设置为false，则首先将整个活动实例序列收集到列表中，然后才发出。默认为true
		 */
		private boolean updateResultsList = true;

		public boolean getRefetchInstances() {
			return refetchInstances;
		}

		public void setRefetchInstances(boolean refetchInstances) {
			this.refetchInstances = refetchInstances;
		}

		public boolean getRepeatHealthCheck() {
			return repeatHealthCheck;
		}

		public void setRepeatHealthCheck(boolean repeatHealthCheck) {
			this.repeatHealthCheck = repeatHealthCheck;
		}

		public Duration getInitialDelay() {
			return initialDelay;
		}

		public void setInitialDelay(Duration initialDelay) {
			this.initialDelay = initialDelay;
		}

		public Duration getRefetchInstancesInterval() {
			return refetchInstancesInterval;
		}

		public void setRefetchInstancesInterval(Duration refetchInstancesInterval) {
			this.refetchInstancesInterval = refetchInstancesInterval;
		}

		public Map<String, String> getPath() {
			return path;
		}

		public void setPath(Map<String, String> path) {
			this.path = path;
		}

		public Duration getInterval() {
			return interval;
		}

		public void setInterval(Duration interval) {
			this.interval = interval;
		}

		public Integer getPort() {
			return port;
		}

		public void setPort(Integer port) {
			this.port = port;
		}

		public boolean isUpdateResultsList() {
			return updateResultsList;
		}

		public void setUpdateResultsList(boolean updateResultsList) {
			this.updateResultsList = updateResultsList;
		}

	}

	/**
	 * 错误重试相关的配置
	 */
	public static class Retry {

		/**
		 * 是否开启重试，默认为true
		 */
		private boolean enabled = true;

		/**
		 * 指示是否对所有类型的操作进行重试，而非仅限于GET请求，默认仅对GET请求重试
		 */
		private boolean retryOnAllOperations = false;

		/**
		 * 指示是否对所有异常进行重试，而非仅限于{@code RetryableException}
		 */
		private boolean retryOnAllExceptions = false;

		/**
		 * 在同一个服务实例上执行重试的最大次数，默认为0，代表该服务执行失败后，便会切换下一个服务进行重试
		 */
		private int maxRetriesOnSameServiceInstance = 0;

		/**
		 * 在下一个服务实例上执行重试的最大次数，默认为1，代表该服务执行失败后，切换到下一个服务执行1次重试
		 */
		private int maxRetriesOnNextServiceInstance = 1;

		/**
		 * 触发重试的状态值集合，这是预留给开发者自定义的场景，需要在服务端
		 */
		private Set<Integer> retryableStatusCodes = new HashSet<>();

		/**
		 * 触发重试的异常集合，默认出现以下异常时重试：
		 * <ul>
		 *     <li>{@link IOException}</li>
		 *     <li>{@link TimeoutException}</li>
		 *     <li>{@link RetryableStatusCodeException}</li>
		 *     <li>{@link org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException}</li>
		 * </ul>
		 */
		private Set<Class<? extends Throwable>> retryableExceptions = new HashSet<>(
				Arrays.asList(IOException.class, TimeoutException.class, RetryableStatusCodeException.class,
						org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException.class));

		/**
		 *
		 * 指定重试达到上限时，触发的回退策略
		 */
		private Backoff backoff = new Backoff();

		/**
		 * Returns true if the load balancer should retry failed requests.
		 * @return True if the load balancer should retry failed requests; false
		 * otherwise.
		 */
		public boolean isEnabled() {
			return this.enabled;
		}

		/**
		 * Sets whether the load balancer should retry failed requests.
		 * @param enabled Whether the load balancer should retry failed requests.
		 */
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isRetryOnAllOperations() {
			return retryOnAllOperations;
		}

		public void setRetryOnAllOperations(boolean retryOnAllOperations) {
			this.retryOnAllOperations = retryOnAllOperations;
		}

		public int getMaxRetriesOnSameServiceInstance() {
			return maxRetriesOnSameServiceInstance;
		}

		public void setMaxRetriesOnSameServiceInstance(int maxRetriesOnSameServiceInstance) {
			this.maxRetriesOnSameServiceInstance = maxRetriesOnSameServiceInstance;
		}

		public int getMaxRetriesOnNextServiceInstance() {
			return maxRetriesOnNextServiceInstance;
		}

		public void setMaxRetriesOnNextServiceInstance(int maxRetriesOnNextServiceInstance) {
			this.maxRetriesOnNextServiceInstance = maxRetriesOnNextServiceInstance;
		}

		public Set<Integer> getRetryableStatusCodes() {
			return retryableStatusCodes;
		}

		public void setRetryableStatusCodes(Set<Integer> retryableStatusCodes) {
			this.retryableStatusCodes = retryableStatusCodes;
		}

		public Set<Class<? extends Throwable>> getRetryableExceptions() {
			return retryableExceptions;
		}

		public void setRetryableExceptions(Set<Class<? extends Throwable>> retryableExceptions) {
			retryableExceptions
				.add(org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException.class);
			this.retryableExceptions = retryableExceptions;
		}

		public Backoff getBackoff() {
			return backoff;
		}

		public void setBackoff(Backoff backoff) {
			this.backoff = backoff;
		}

		public boolean isRetryOnAllExceptions() {
			return retryOnAllExceptions;
		}

		public void setRetryOnAllExceptions(boolean retryOnAllExceptions) {
			this.retryOnAllExceptions = retryOnAllExceptions;
		}

		/**
		 * 回退策略，当重试达到上限时，便会触发回退
		 */
		public static class Backoff {

			/**
			 * 是否启用回退策略，默认为false，代表不启用
			 */
			private boolean enabled = false;

			/**
			 * 最小的回退间隔，默认为5ms，
			 * 被用于设置到{@link RetryBackoffSpec#minBackoff}
			 */
			private Duration minBackoff = Duration.ofMillis(5);

			/**
			 * 最大的回退间隔，默认为最大值，
			 * 被用于设置到{@link RetryBackoffSpec#maxBackoff}
			 */
			private Duration maxBackoff = Duration.ofMillis(Long.MAX_VALUE);

			/**
			 *
			 * Used to set {@link RetryBackoffSpec#jitter}.
			 */
			private double jitter = 0.5d;

			public Duration getMinBackoff() {
				return minBackoff;
			}

			public void setMinBackoff(Duration minBackoff) {
				this.minBackoff = minBackoff;
			}

			public Duration getMaxBackoff() {
				return maxBackoff;
			}

			public void setMaxBackoff(Duration maxBackoff) {
				this.maxBackoff = maxBackoff;
			}

			public double getJitter() {
				return jitter;
			}

			public void setJitter(double jitter) {
				this.jitter = jitter;
			}

			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

		}

	}

	public static class Subset {

		/**
		 * Instance id of deterministic subsetting. If not set,
		 * {@link IdUtils#getDefaultInstanceId(PropertyResolver)} will be used.
		 */
		private String instanceId = "";

		/**
		 * Max subset size of deterministic subsetting.
		 */
		private int size = 100;

		public String getInstanceId() {
			return instanceId;
		}

		public void setInstanceId(String instanceId) {
			this.instanceId = instanceId;
		}

		public int getSize() {
			return size;
		}

		public void setSize(int size) {
			this.size = size;
		}

	}

	public static class Stats {

		/**
		 * Indicates whether the {@code path} should be added to {@code uri} tag in
		 * metrics. When {@link RestTemplate} is used to execute load-balanced requests
		 * with high cardinality paths, setting it to {@code false} is recommended.
		 */
		private boolean includePath = true;

		public boolean isIncludePath() {
			return includePath;
		}

		public void setIncludePath(boolean includePath) {
			this.includePath = includePath;
		}

	}

}
