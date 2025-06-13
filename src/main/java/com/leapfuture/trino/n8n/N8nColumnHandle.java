package com.leapfuture.trino.n8n;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.type.Type;

import static java.util.Objects.requireNonNull;

/**
 * N8N Column Handle
 */
public class N8nColumnHandle implements ColumnHandle {
    
    private final String name;
    private final Type type;
    private final int ordinalPosition;
    
    @JsonCreator
    public N8nColumnHandle(
            @JsonProperty("name") String name,
            @JsonProperty("type") Type type,
            @JsonProperty("ordinalPosition") int ordinalPosition) {
        this.name = requireNonNull(name, "name is null");
        this.type = requireNonNull(type, "type is null");
        this.ordinalPosition = ordinalPosition;
    }
    
    @JsonProperty
    public String getName() {
        return name;
    }
    
    @JsonProperty
    public Type getType() {
        return type;
    }
    
    @JsonProperty
    public int getOrdinalPosition() {
        return ordinalPosition;
    }
    
    @Override
    public String toString() {
        return name + ":" + type;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        N8nColumnHandle other = (N8nColumnHandle) obj;
        return name.equals(other.name) && 
               type.equals(other.type) && 
               ordinalPosition == other.ordinalPosition;
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
} 