package org.example.igniteoperator.utils.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Objects;

@Data
public class IgniteSaSpec {
    private String googleServiceAccount = null;
    
    @JsonIgnore
    public boolean isBindToGoogleSa() {
        return Objects.isNull(googleServiceAccount) || googleServiceAccount.isBlank();
    }
}
