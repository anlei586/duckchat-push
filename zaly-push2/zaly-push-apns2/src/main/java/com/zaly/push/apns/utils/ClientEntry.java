package com.zaly.push.apns.utils;

import java.util.Map.Entry;

public class ClientEntry<K, V> implements Entry<K, V> {

	private K key;
	private V value;

	public ClientEntry(K key, V val) {

		this.key = key;
		this.value = val;
	}

	public K getKey() {
		// TODO Auto-generated method stub
		return key;
	}

	public V getValue() {
		return value;
	}

	public V setValue(V value) {
		// TODO Auto-generated method stub
		return this.value = value;
	}

}
