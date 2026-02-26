package net.unit8.rodriguez.fuse;

import net.unit8.rodriguez.fuse.fault.*;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FaultRuleTest {

    @Test
    void matchesPathAndOperation() {
        FaultRule rule = new FaultRule(".*\\.log$", Set.of(FuseOperation.WRITE), new DiskFull());

        assertThat(rule.matches("/var/app.log", FuseOperation.WRITE)).isTrue();
        assertThat(rule.matches("/var/app.log", FuseOperation.READ)).isFalse();
        assertThat(rule.matches("/var/app.txt", FuseOperation.WRITE)).isFalse();
    }

    @Test
    void matchesAllOperationsWhenNull() {
        FaultRule rule = new FaultRule(".*", null, new IOError());

        assertThat(rule.matches("/any/path", FuseOperation.READ)).isTrue();
        assertThat(rule.matches("/any/path", FuseOperation.WRITE)).isTrue();
        assertThat(rule.matches("/any/path", FuseOperation.FSYNC)).isTrue();
    }

    @Test
    void matchesAllPathsWhenNull() {
        FaultRule rule = new FaultRule(null, Set.of(FuseOperation.WRITE), new DiskFull());

        assertThat(rule.matches("/any/path", FuseOperation.WRITE)).isTrue();
        assertThat(rule.matches("/other/path", FuseOperation.WRITE)).isTrue();
        assertThat(rule.matches("/any/path", FuseOperation.READ)).isFalse();
    }
}
