package org.example.igniteoperator.utils.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolumeSpec {
    private String name;
    private String mountPath;
    private List<String> accessModes;
    private String storage;
    
    @Builder.Default
    private String storageClassName = "standard";
    
    public static VolumeSpec defaultDataVolume() {
        return new VolumeSpecBuilder()
                .name("work-vol")
                .mountPath("/opt/gridgain/work")
                .accessModes(List.of("ReadWriteOnce"))
                .storage("1Gi")
                .build();
    }
    
    public static VolumeSpec defaultWalVolume() {
        return new VolumeSpecBuilder()
                .name("wal-vol")
                .mountPath("/opt/gridgain/wal")
                .accessModes(List.of("ReadWriteOnce"))
                .storage("1Gi")
                .build();
    }
    
    public static VolumeSpec defaultWalarchiveVolume() {
        return new VolumeSpecBuilder()
                .name("walarchive-vol")
                .mountPath("/opt/gridgain/walarchive")
                .accessModes(List.of("ReadWriteOnce"))
                .storage("1Gi")
                .build();
    }
}
