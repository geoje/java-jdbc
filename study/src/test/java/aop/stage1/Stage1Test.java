package aop.stage1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import aop.DataAccessException;
import aop.StubUserHistoryDao;
import aop.domain.User;
import aop.repository.UserDao;
import aop.repository.UserHistoryDao;
import aop.service.AppUserService;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Stage1Test {

    private static final Logger log = LoggerFactory.getLogger(Stage1Test.class);

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserHistoryDao userHistoryDao;

    @Autowired
    private StubUserHistoryDao stubUserHistoryDao;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @BeforeEach
    void setUp() {
        final var user = new User("gugu", "password", "hkkang@woowahan.com");
        userDao.insert(user);
    }

    @Test
    void testChangePassword() {
        // given
        UserService userService = new UserService(userDao, userHistoryDao);
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.addAdvisor(new TransactionAdvisor(platformTransactionManager));
        proxyFactoryBean.setProxyTargetClass(true);
        proxyFactoryBean.setTarget(userService);

        // when
        UserService bean = (UserService) proxyFactoryBean.getObject();
        final var newPassword = "qqqqq";
        final var createBy = "gugu";
        Objects.requireNonNull(bean).changePassword(1L, newPassword, createBy);

        final var actual = bean.findById(1L);

        // then
        assertThat(actual.getPassword()).isEqualTo(newPassword);
    }

    @Test
    void testTransactionRollback() {
        // given
        UserService userService = new UserService(userDao, stubUserHistoryDao);
        ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
        proxyFactoryBean.addAdvisor(new TransactionAdvisor(platformTransactionManager));
        proxyFactoryBean.setProxyTargetClass(true);
        proxyFactoryBean.setTarget(userService);

        // when
        UserService bean = (UserService) proxyFactoryBean.getObject();
        final var newPassword = "newPassword";
        final var createBy = "gugu";
        assertThrows(DataAccessException.class,
                () -> Objects.requireNonNull(bean).changePassword(1L, newPassword, createBy));

        final var actual = Objects.requireNonNull(bean).findById(1L);

        assertThat(actual.getPassword()).isNotEqualTo(newPassword);
    }
}
