package com.gitforge.util;

import com.gitforge.model.Commit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkedListTest {

    private LinkedList<Commit> history;

    @BeforeEach
    void setUp() {
        history = new LinkedList<>();
    }

    @Test
    void insertTraverseAndFind() {
        Commit first = commit(1L, "aaa");
        Commit second = commit(2L, "bbb");

        history.insertLast(first);
        history.insertFirst(second);

        List<Commit> traversed = history.toList();
        assertEquals(List.of(2L, 1L), traversed.stream().map(Commit::getId).toList());

        assertTrue(history.find(c -> c.getId() == 1L).isPresent());
        assertTrue(history.find(c -> "bbb".equals(c.getHash())).isPresent());
    }

    @Test
    void deleteCommitFromHistory() {
        Commit first = commit(1L, "aaa");
        Commit second = commit(2L, "bbb");
        history.insertLast(first);
        history.insertLast(second);

        assertTrue(history.deleteIf(c -> c.getId() == 1L));
        assertEquals(1, history.size());
        assertFalse(history.find(c -> c.getId() == 1L).isPresent());
    }

    @Test
    void traverseCollectsHistoryInOrder() {
        List<Long> ids = new ArrayList<>();
        history.insertLast(commit(1L, "a"));
        history.insertLast(commit(2L, "b"));
        history.insertFirst(commit(3L, "c"));

        history.traverse(commit -> ids.add(commit.getId()));
        assertEquals(List.of(3L, 1L, 2L), ids);
    }

    private static Commit commit(long id, String hash) {
        Commit commit = new Commit();
        commit.setId(id);
        commit.setHash(hash);
        commit.setRepositoryId(1L);
        commit.setBranchId(1L);
        commit.setMessage("msg-" + id);
        return commit;
    }
}
