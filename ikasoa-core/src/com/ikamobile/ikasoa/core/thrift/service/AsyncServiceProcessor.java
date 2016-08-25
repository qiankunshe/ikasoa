package com.ikamobile.ikasoa.core.thrift.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.thrift.AsyncProcessFunction;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.TBaseAsyncProcessor;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.server.AbstractNonblockingServer.AsyncFrameBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ikamobile.ikasoa.core.STException;
import com.ikamobile.ikasoa.core.thrift.service.base.ArgsThriftBase;
import com.ikamobile.ikasoa.core.thrift.service.base.ResultThriftBase;
import com.ikamobile.ikasoa.core.thrift.service.impl.CallBack;

/**
 * 异步服务处理器
 * 
 * @author <a href="mailto:larry7696@gmail.com">Larry</a>
 * @version 0.4.2
 */
public class AsyncServiceProcessor extends TBaseAsyncProcessor<AsyncService> implements SProcessor {

	@SuppressWarnings("rawtypes")
	public AsyncServiceProcessor(AsyncService service) {
		super(service, getProcessMap(new HashMap<String, AsyncProcessFunction<AsyncService, ? extends TBase, ?>>()));
	}

	@SuppressWarnings("rawtypes")
	private static Map<String, AsyncProcessFunction<AsyncService, ? extends TBase, ?>> getProcessMap(
			Map<String, AsyncProcessFunction<AsyncService, ? extends TBase, ?>> processMap) {
		processMap.put(FUNCTION_NAME, new GetAsyncProcessFunction());
		return processMap;
	}

	public static class GetAsyncProcessFunction extends AsyncProcessFunction<AsyncService, ArgsThriftBase, CallBack> {

		private static final Logger LOG = LoggerFactory.getLogger(GetAsyncProcessFunction.class);

		public GetAsyncProcessFunction() {
			super(FUNCTION_NAME);
		}

		public ArgsThriftBase getEmptyArgsInstance() {
			return new ArgsThriftBase();
		}

		public AsyncMethodCallback<String> getResultHandler(final AsyncFrameBuffer fb, final int seqid) {
			final AsyncProcessFunction<AsyncService, ArgsThriftBase, CallBack> fcall = this;
			return new AsyncMethodCallback<String>() {
				public void onComplete(String o) {
					ResultThriftBase result = new ResultThriftBase();
					result.setFieldValue(null, o);
					try {
						fcall.sendResponse(fb, result, TMessageType.REPLY, seqid);
						return;
					} catch (Exception e) {
						LOG.error("Exception writing to internal frame buffer", e);
					}
					fb.close();
				}

				public void onError(Exception e) {
					byte msgType = TMessageType.REPLY;
					TBase<?, ?> msg;
					{
						msgType = TMessageType.EXCEPTION;
						msg = (TBase<?, ?>) new TApplicationException(TApplicationException.INTERNAL_ERROR,
								e.getMessage());
					}
					try {
						fcall.sendResponse(fb, msg, msgType, seqid);
						return;
					} catch (Exception ex) {
						LOG.error("Exception writing to internal frame buffer", ex);
					}
					fb.close();
				}
			};
		}

		protected boolean isOneway() {
			return false;
		}

		public void start(AsyncService service, ArgsThriftBase args, AsyncMethodCallback<CallBack> resultHandler)
				throws TException {
			LOG.debug("Args is : " + args.getStr());
			try {
				service.get((String) args.getStr(), resultHandler);
			} catch (STException e) {
				throw new TException(e);
			}
		}
	}

}
