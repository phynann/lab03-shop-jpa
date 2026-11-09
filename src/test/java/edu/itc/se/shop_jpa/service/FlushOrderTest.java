package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Tag;
import edu.itc.se.shop_jpa.repository.TagRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class FlushOrderTest {

    @Autowired
    TagRepository tags;

    @Test
    @Transactional
    void insertBeforeDeleteOnFlushViolatesUniqueConstraint() {
        Long id = tags.saveAndFlush(new Tag("sale")).getId();
        Tag existing = tags.findById(id).orElseThrow();

        // Hibernate executes INSERTs before DELETEs at flush time: the new 'sale'
        // row is inserted while the old 'sale' row still exists.
        tags.delete(existing);
        tags.save(new Tag("sale"));

        assertThatThrownBy(tags::flush)
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void flushingBetweenDeleteAndInsertExecutesDeleteFirst() {
        Long id = tags.saveAndFlush(new Tag("sale")).getId();
        Tag existing = tags.findById(id).orElseThrow();

        tags.delete(existing);
        tags.flush(); // or em.flush(): the DELETE hits the database before the INSERT
        tags.save(new Tag("sale"));
        tags.flush();

        assertThat(tags.countByName("sale")).isEqualTo(1);
    }
}
