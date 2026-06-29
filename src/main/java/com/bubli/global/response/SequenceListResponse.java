package com.bubli.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SequenceListResponse<T> {

    private final List<T> items;
    private final Long lastReceivedSequence;
    private final long latestSequence;
    private final boolean hasNext;
}
