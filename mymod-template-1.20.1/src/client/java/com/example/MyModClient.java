package com.example;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class MyModClient implements ClientModInitializer {
    // 功能1：1-9循環 (F8觸發)
    private enum Function1State {
        PRESS_KEY,
        RIGHT_CLICK,
        FINAL_WAIT
    }
    
    // 功能2：1-5循環 (F7觸發)
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
            // 處理按鍵觸發
            if (startFunction1Key.wasPressed()) {
                isFunction1Running = true;
                isFunction2Running = false; // 確保兩個功能不會同時運行
                function1CurrentKey = 1;
                function1State = Function1State.PRESS_KEY;
                function1WaitTicks = 0;
            }
            
            if (startFunction2Key.wasPressed()) {
                isFunction2Running = true;
                isFunction1Running = false; // 確保兩個功能不會同時運行
                function2CurrentKey = 1;
                function2State = Function2State.PRESS_KEY;
                function2WaitTicks = 0;
            }
            
            if (stopKey.wasPressed()) {
                isFunction1Running = false;
                isFunction2Running = false;
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

    private void tickFunction1(MinecraftClient client) {
        if (function1WaitTicks > 0) {
            function1WaitTicks--;
            return;
        }

        switch (function1State) {
            case PRESS_KEY:
                // 設置快捷欄位置
                if (client.player != null) {
                    client.player.getInventory().selectedSlot = function1CurrentKey - 1;
                }
                function1State = Function1State.RIGHT_CLICK;
                function1WaitTicks = 2; // 等待2tick
                break;

            case RIGHT_CLICK:
                // 模擬右鍵點擊
                if (client.interactionManager != null && client.player != null) {
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                }
                
                if (function1CurrentKey < 9) {
                    function1CurrentKey++;
                    function1State = Function1State.PRESS_KEY;
                } else {
                    // 所有按鍵完成後，按下1並等待3秒
                    function1State = Function1State.FINAL_WAIT;
                }
                function1WaitTicks = 2; // 等待2tick
                break;

            case FINAL_WAIT:
                // 按下1
                if (client.player != null) {
                    client.player.getInventory().selectedSlot = 0; // 1對應位置0
                }
                
                // 等待3秒 (60tick)
                function1WaitTicks = 60;
                
                // 重置狀態開始新循環
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
                // 設置快捷欄位置
                if (client.player != null) {
                    client.player.getInventory().selectedSlot = function2CurrentKey - 1;
                }
                function2State = Function2State.RIGHT_CLICK;
                function2WaitTicks = 2; // 等待2tick
                break;

            case RIGHT_CLICK:
                // 模擬右鍵點擊
                if (client.interactionManager != null && client.player != null) {
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                }
                
                if (function2CurrentKey < 5) {
                    function2CurrentKey++;
                    function2State = Function2State.PRESS_KEY;
                } else {
                    // 所有按鍵完成後，按下2並等待3秒
                    function2State = Function2State.FINAL_WAIT;
                }
                function2WaitTicks = 2; // 等待2tick
                break;

            case FINAL_WAIT:
                // 按下2
                if (client.player != null) {
                    client.player.getInventory().selectedSlot = 1; // 2對應位置1
                }
                
                // 等待3秒 (60tick)
                function2WaitTicks = 60;
                
                // 重置狀態開始新循環
                function2CurrentKey = 1;
                function2State = Function2State.PRESS_KEY;
                break;
        }
    }
}