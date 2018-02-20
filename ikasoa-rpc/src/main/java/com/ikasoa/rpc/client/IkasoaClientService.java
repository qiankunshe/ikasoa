package com.ikasoa.rpc.client;

import com.ikasoa.core.STException;
import com.ikasoa.core.thrift.Factory;
import com.ikasoa.core.thrift.client.ThriftClient;
import com.ikasoa.rpc.BaseGetService;
import com.ikasoa.rpc.IkasoaException;
import com.ikasoa.rpc.handler.ClientInvocationContext;
import com.ikasoa.rpc.handler.ClientInvocationHandler;
import com.ikasoa.rpc.handler.ProtocolHandler;

/**
 * 客户端服务
 * 
 * @author <a href="mailto:larry7696@gmail.com">Larry</a>
 * @version 0.1
 */
public class IkasoaClientService<T, R> implements BaseGetService<T, R> {

	private Factory factory;

	private ThriftClient thriftClient;

	protected String serviceKey = null;

	protected ProtocolHandler<T, R> protocolHandler;

	// TODO: Larry
	protected ClientInvocationHandler invocationHandler;

	public IkasoaClientService(Factory factory, ThriftClient thriftClient, ProtocolHandler<T, R> protocolHandler) {
		this.factory = factory;
		this.thriftClient = thriftClient;
		this.protocolHandler = protocolHandler;
	}

	public IkasoaClientService(Factory factory, ThriftClient thriftClient, String serviceKey,
			ProtocolHandler<T, R> protocolHandler, ClientInvocationHandler invocationHandler) {
		this.factory = factory;
		this.thriftClient = thriftClient;
		this.serviceKey = serviceKey;
		this.protocolHandler = protocolHandler;
		this.invocationHandler = invocationHandler;
	}

	@Override
	public R get(T arg) throws Throwable {
		ClientInvocationContext context = null;
		if (invocationHandler != null) {
			context = new ClientInvocationContext();
			context.setServerHost(thriftClient.getServerHost());
			context.setServerPort(thriftClient.getServerPort());
			context.setServiceKey(serviceKey);
			context.setArgObject(arg);
			context = invocationHandler.before(context);
		}
		// 参数转换
		if (protocolHandler == null)
			throw new IkasoaException("'protocolHandler' is null !");
		String argStr = null;
		try {
			argStr = protocolHandler.argToStr(arg);
		} catch (Throwable t) {
			throw new IkasoaException("Execute 'argToStr' function exception !", t);
		}
		if (context != null && invocationHandler != null) {
			context.setArgStr(argStr);
			argStr = invocationHandler.invoke(context).getArgStr();
		}
		// 执行操作,获取返回值
		String resultStr = "";
		try {
			resultStr = factory.getService(thriftClient, serviceKey).get(argStr);
		} catch (STException e) {
			throw new IkasoaException("Thrift get exception !", e);
		} finally {
			thriftClient.close();
		}
		if (context != null && invocationHandler != null)
			context.setResultStr(resultStr);
		// 返回值转换
		Throwable throwable = null;
		try {
			throwable = protocolHandler.strToThrowable(resultStr);
		} catch (Throwable t) {
			throw new IkasoaException("Execute 'strToThrowable' function exception !", t);
		}
		// 判断是否为异常返回,如果是就抛出异常
		if (throwable != null) {
			if (invocationHandler != null) {
				invocationHandler.exception(context, throwable);
				context = null;
			}
			throw throwable;
		}
		// 不是异常返回就返回正常值
		try {
			R result = protocolHandler.strToResult(resultStr);
			if (context != null && invocationHandler != null) {
				context.setResultObject(result);
				invocationHandler.after(context);
				context = null;
			}
			return result;
		} catch (Throwable t) {
			throw new IkasoaException("Execute 'strToResult' function exception !", t);
		}
	}

	public ThriftClient getThriftClient() {
		return thriftClient;
	}

	public void setThriftClient(ThriftClient thriftClient) {
		this.thriftClient = thriftClient;
	}

}
