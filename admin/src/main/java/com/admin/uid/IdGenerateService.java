package com.admin.uid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 唯一ID生成服务，使用 Twitter snowflake 算法
 * 机房ID：固定为0，占用2位
 * 机器ID：由 ServerIdProvider 提供
 *
 * @author tjq
 * @since 2020/4/6
 */
@Slf4j
@Service
public class IdGenerateService {

    private final SnowFlakeIdGenerator snowFlakeIdGenerator;


    public IdGenerateService() {
        snowFlakeIdGenerator = new SnowFlakeIdGenerator(0L, getMachineId());
    }

    /**
     * 分配分布式唯一ID
     * @return 分布式唯一ID
     */
    public long allocate() {
        return snowFlakeIdGenerator.nextId();
    }


    public static long getMachineId() {
        try {
            byte[] mac = getMacAddress();
            return Math.abs((mac[3] + mac[4] + mac[5]) % 32); // 取后3个字节的和并取模
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Machine ID from MAC Address", e);
        }
    }

    private static byte[] getMacAddress() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface network = networkInterfaces.nextElement();
            byte[] mac = network.getHardwareAddress();
            if (mac != null) {
                return mac;
            }
        }
        throw new SocketException("No MAC address found.");
    }
}
