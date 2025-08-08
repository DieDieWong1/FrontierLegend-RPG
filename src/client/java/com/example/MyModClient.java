package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class MyModClient implements ClientModInitializer {
    // 狀態枚舉
    private enum Function1State {
        PRESS_KEY,
        RIGHT_CLICK,
        FINAL_WAIT,
        HEALTH_WAIT  // 新增：血量等待狀態
    }
    
    private enum Function2State {
        PRESS_KEY,
        RIGHT_CLICK,
        FINAL_WAIT
    }
    
    // 按鍵綁定
    private KeyBinding startFunction1Key;
    private KeyBinding startFunction2Key;
    private KeyBinding stopKey;
    
    // 功能1狀態變量
    private boolean isFunction1Running = false;
    private int function1CurrentKey = 1;
    private Function1State function1State = Function1State.PRESS_KEY;
    private int function1WaitTicks = 0;
    
    // 功能2狀態變量
    private boolean isFunction2Running = false;
    private int function2CurrentKey = 1;
    private Function2State function2State = Function2State.PRESS_KEY;
    private int function2WaitTicks = 0;
    
    // 新增：血量保護變量
    private boolean isHealthLow = false;
    private int healthCheckCooldown = 0;

    @Override
    public void onInitializeClient() {
        // 註冊按鍵綁定
        startFunction1Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoclicker.function1",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "key.category.autoclicker"
        ));

        startFunction2Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoclicker.function2",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                "key.category.autoclicker"
        ));

        stopKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoclicker.stop",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F12,
                "key.category.autoclicker"
        ));

        // 註冊客戶端tick事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 新增：全局停止條件檢查
            if (shouldStopAll(client)) {
                stopAllFunctions();
                return;
            }
            
            // 新增：血量保護檢查（每10tick檢查一次）
            healthCheckCooldown--;
            if (healthCheckCooldown <= 0) {
                checkPlayerHealth(client);
                healthCheckCooldown = 10; // 每半秒檢查一次
            }
            
            // 處理按鍵觸發
            if (startFunction1Key.wasPressed()) {
                startFunction1();
            }
            
            if (startFunction2Key.wasPressed()) {
                startFunction2();
            }
            
            if (stopKey.wasPressed()) {
                stopAllFunctions();
            }

            // 執行當前活動的功能
            if (isFunction1Running) {
                tickFunction1(client);
            }
            
            if (isFunction2Running) {
                tickFunction2(client);
            }
        });
    }
    
    // 新增：全局停止條件檢查
    private boolean shouldStopAll(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        
        // 玩家死亡
        if (player != null && player.isDead()) {
            return true;
        }
        
        // 退出遊戲
        if (world == null || player == null) {
            return true;
        }
        
        // 高延遲（>100ms）
        if (client.getNetworkHandler() != null && client.getNetworkHandler().getPlayerListEntry(player.getUuid()) != null) {
            int ping = client.getNetworkHandler().getPlayerListEntry(player.getUuid()).getLatency();
            if (ping > 100) {
                return true;
            }
        }
        
        return false;
    }
    
    // 新增：血量保護檢查
    private void checkPlayerHealth(MinecraftClient client) {
        if (client.player == null) return;
        
        float health = client.player.getHealth();
        float maxHealth = client.player.getMaxHealth();
        float healthThreshold = maxHealth * 0.4f; // 40%血量（4格心）
        float resumeThreshold = maxHealth; // 100%血量（10格心）
        
        // 低於4格心時觸發保護
        if (isFunction1Running && health <= healthThreshold) {
            isHealthLow = true;
            isFunction1Running = false;
            
            // 切換到第7欄位
            if (client.player != null) {
                client.player.getInventory().selectedSlot = 6; // 第7欄位（索引6）
            }
        }
        
        // 血量恢復後重新開始功能1
        if (isHealthLow && health >= resumeThreshold) {
            isHealthLow = false;
            startFunction1();
        }
    }
    
    // 新增：啟動功能1（封裝）
    private void startFunction1() {
        isFunction1Running = true;
        isFunction2Running = false;
        function1CurrentKey = 1;
        function1State = Function1State.PRESS_KEY;
        function1WaitTicks = 0;
        isHealthLow = false; // 重置血量保護標誌
    }
    
    // 新增：啟動功能2（封裝）
    private void startFunction2() {
        isFunction2Running = true;
        isFunction1Running = false;
        function2CurrentKey = 1;
        function2State = Function2State.PRESS_KEY;
        function2WaitTicks = 0;
    }
    
    // 新增：停止所有功能（封裝）
    private void stopAllFunctions() {
        isFunction1Running = false;
        isFunction2Running = false;
        isHealthLow = false; // 重置血量保護標誌
    }

    private void tickFunction1(MinecraftClient client) {
        if (function1WaitTicks > 0) {
            function1WaitTicks--;
            return;
        }

        switch (function1State) {
            case PRESS_KEY:
                if (client.player != null) {
                    client.player.getInventory().selectedSlot = function1CurrentKey - 1;
                }
                function1State = Function1State.RIGHT_CLICK;
                function1WaitTicks = 2;
                break;

            case RIGHT_CLICK:
                if (client.interactionManager != null && client.player != null) {
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                }
                
                if (function1CurrentKey < 9) {
                    function1CurrentKey++;
                    function1State = Function1State.PRESS_KEY;
                } else {
                    function1State = Function1State.FINAL_WAIT;
                }
                function1WaitTicks = 2;
                break;

            case FINAL_WAIT:
                if (client.player != null) {
                    client.player.getInventory().selectedSlot = 0;
                }
                function1WaitTicks = 60;
                function1CurrentKey = 1;
                function1State = Function1State.PRESS_KEY;
                break;
        }
    }

    private void tickFunction2(MinecraftClient client) {
        if (function2WaitTicks > 0) {
            function2WaitTicks--;
            return;
        }

        switch (function2State) {
            case PRESS_KEY:
                if (client.player != null) {
                    client.player.getInventory().selectedSlot = function2CurrentKey - 1;
                }
                function2State = Function2State.RIGHT_CLICK;
                function2WaitTicks = 2;
                break;

            case RIGHT_CLICK:
                if (client.interactionManager != null && client.player != null) {
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                }
                
                if (function2CurrentKey < 5) {
                    function2CurrentKey++;
                    function2State = Function2State.PRESS_KEY;
                } else {
                    function2State = Function2State.FINAL_WAIT;
                }
                function2WaitTicks = 2;
                break;

            case FINAL_WAIT:
                if (client.player != null) {
                    client.player.getInventory().selectedSlot = 1;
                }
                function2WaitTicks = 60;
                function2CurrentKey = 1;
                function2State = Function2State.PRESS_KEY;
                break;
        }
    }
}