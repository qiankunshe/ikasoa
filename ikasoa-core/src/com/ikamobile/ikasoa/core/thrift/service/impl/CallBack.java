package com.ikamobile.ikasoa.core.thrift.service.impl;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncClient;
import org.apache.thrift.async.TAsyncMethodCall;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TMemoryInputTransport;
import org.apache.thrift.transport.TNonblockingTransport;

import com.ikamobile.ikasoa.core.thrift.service.Processor;
import com.ikamobile.ikasoa.core.thrift.service.base.AbstractThriftBase;
import com.ikamobile.ikasoa.core.thrift.service.base.ArgsThriftBase;

/**
 * 异步回调对象
 * 
 * @author <a href="mailto:larry7696@gmail.com">Larry</a>
 * @version 0.4.3
 */
public class CallBack extends TAsyncMethodCall<CallBack> {

	private String arg;

	public CallBack(String arg, AsyncMethodCallback<CallBack> resultHandler, TAsyncClient client,
			TProtocolFactory protocolFactory, TNonblockingTransport transport) throws TException {
		super(client, protocolFactory, transport, resultHandler, false);
		this.arg = arg;
	}

	public void write_args(TProtocol prot) throws TException {
		prot.writeMessageBegin(new TMessage(Processor.FUNCTION_NAME, TMessageType.CALL, 0));
		ArgsThriftBase args = new ArgsThriftBase();
		args.setFieldValue(AbstractThriftBase.FieldsEnum.VALUE, arg);
		args.write(prot);
		prot.writeMessageEnd();
	}

	public String getResult() throws TException {
		if (getState() != TAsyncMethodCall.State.RESPONSE_READ) {
			throw new IllegalStateException("Method call not finished!");
		}
		TMemoryInputTransport memoryTransport = new TMemoryInputTransport(getFrameBuffer().array());
		TProtocol prot = client.getProtocolFactory().getProtocol(memoryTransport);
		return (new ServiceClientImpl(prot)).recvGet();
	}
}