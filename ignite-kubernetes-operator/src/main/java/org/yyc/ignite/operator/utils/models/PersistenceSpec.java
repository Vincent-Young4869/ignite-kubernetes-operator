package org.yyc.ignite.operator.utils.models;

import lombok.Data;

@Data
public class PersistenceSpec {
    private boolean persistenceEnabled = false;
    
    private VolumeSpec dataVolumeSpec = VolumeSpec.defaultDataVolume();
    private VolumeSpec walVolumeSpec = VolumeSpec.defaultWalVolume();
    private VolumeSpec walArchiveVolumeSpec = VolumeSpec.defaultWalarchiveVolume();
}
