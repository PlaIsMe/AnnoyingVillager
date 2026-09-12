package com.pla.annoyingvillagers.clazz;

public interface PersistentPlayerNpc {
    String persistentPlayerIdentity();

    @javax.annotation.Nullable
    default com.pla.annoyingvillagers.util.NpcTabSkin tabSkin() {
        return com.pla.annoyingvillagers.util.NpcTabSkin.forIdentity(persistentPlayerIdentity());
    }
}
