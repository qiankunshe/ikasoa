package com.ikasoa.core.loadbalance.impl;

import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.ikasoa.core.STException;
import com.ikasoa.core.loadbalance.LoadBalance;
import com.ikasoa.core.loadbalance.ServerInfo;
import com.ikasoa.core.utils.StringUtil;

/**
 * 一致性Hash负载均衡实现
 * 
 * @author <a href="mailto:larry7696@gmail.com">Larry</a>
 * @version 0.5
 */
public class ConsistencyHashLoadBalanceImpl implements LoadBalance {

	private TreeMap<Long, ServerInfo> nodes = null;

	/**
	 * 设置虚拟节点数目
	 */
	private int VIRTUAL_NUM = 4;

	private SoftReference<String> hashReference;

	public ConsistencyHashLoadBalanceImpl(List<ServerInfo> serverInfoList, String hash) {
		if (StringUtil.isEmpty(hash))
			throw new RuntimeException("Constructor must exist hash parameter !");
		try {
			hashReference = new SoftReference<String>(InetAddress.getLocalHost().getHostAddress() + hash);
			nodes = new TreeMap<>();
			for (int i = 0; i < serverInfoList.size(); i++) {
				ServerInfo serverInfo = serverInfoList.get(i);
				for (int j = 0; j < VIRTUAL_NUM; j++)
					nodes.put(hash(computeMd5("SHARD-" + i + "-NODE-" + j), j), serverInfo);
			}
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ServerInfo getServerInfo() {
		Long key = hash(computeMd5(hashReference.get()), 0);
		SortedMap<Long, ServerInfo> tailMap = nodes.tailMap(key);
		key = tailMap.isEmpty() ? nodes.firstKey() : tailMap.firstKey();
		return nodes.get(key);
	}

	@Override
	public ServerInfo next() throws STException {
		return getServerInfo();
	}

	/**
	 * 根据2^32把节点分布到圆环上面。
	 */
	private long hash(byte[] digest, int nTime) {
		long rv = ((long) (digest[3 + nTime * 4] & 0xFF) << 24) | ((long) (digest[2 + nTime * 4] & 0xFF) << 16)
				| ((long) (digest[1 + nTime * 4] & 0xFF) << 8) | (digest[0 + nTime * 4] & 0xFF);
		return rv & 0xffffffffL; // Truncate to 32-bits
	}

	/**
	 * Get the md5 of the given key.
	 */
	private byte[] computeMd5(String value) {
		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("MD5 not supported", e);
		}
		md5.reset();
		byte[] keyBytes = null;
		try {
			keyBytes = value.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unknown string :" + value, e);
		}
		md5.update(keyBytes);
		return md5.digest();
	}

}
