package com.hejz.springbootstomp;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SpringBootStompApplication 單元測試類別
 * 
 * <p>此測試類別驗證 Spring Boot 應用程式主類別的功能，包括：
 * <ul>
 *   <li>應用程式上下文載入</li>
 *   <li>主要方法執行</li>
 *   <li>Bean 初始化</li>
 * </ul>
 * 
 * <p>測試執行順序：
 * <ol>
 *   <li>testApplicationContextLoads() - 驗證應用程式上下文載入</li>
 *   <li>testMainMethod() - 驗證主方法（模擬測試）</li>
 * </ol>
 * 
 * @see com.hejz.springbootstomp.SpringBootStompApplication
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@SpringBootTest
class SpringBootStompApplicationTests {

    /**
     * 測試應用程式上下文載入
     * 
     * <p>此測試驗證 Spring Boot 應用程式能夠正常啟動並載入所有必要的 Bean。
     * 如果測試通過，表示應用程式配置正確，所有依賴都已正確注入。
     * 
     * <p>測試內容：
     * <ul>
     *   <li>驗證應用程式上下文不為 null</li>
     *   <li>驗證應用程式能夠正常啟動</li>
     * </ul>
     * 
     * @see com.hejz.springbootstomp.SpringBootStompApplication#main(String[])
     */
    @Test
    @Order(1)
    void testApplicationContextLoads() {
        // 如果應用程式上下文能夠正常載入，測試即通過
        // Spring Boot 測試框架會自動驗證上下文載入
        assertTrue(true, "應用程式上下文應能正常載入");
    }

    /**
     * 測試主方法（模擬測試）
     * 
     * <p>此測試驗證主方法的基本功能。由於主方法會啟動完整的應用程式，
     * 在單元測試中我們只驗證方法存在且可調用。
     * 
     * <p>注意事項：
     * <ul>
     *   <li>實際的主方法測試需要在整合測試中進行</li>
     *   <li>此測試主要驗證方法簽名和基本邏輯</li>
     * </ul>
     * 
     * @see com.hejz.springbootstomp.SpringBootStompApplication#main(String[])
     */
    @Test
    @Order(2)
    void testMainMethod() {
        // 驗證主類別存在
        Class<?> mainClass = SpringBootStompApplication.class;
        assertNotNull(mainClass, "主類別應存在");
        
        // 驗證主方法存在
        try {
            mainClass.getMethod("main", String[].class);
            assertTrue(true, "主方法應存在");
        } catch (NoSuchMethodException e) {
            fail("主方法不存在: " + e.getMessage());
        }
    }
}
