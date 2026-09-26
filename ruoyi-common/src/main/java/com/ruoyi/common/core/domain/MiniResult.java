package com.ruoyi.common.core.domain;

/**
 * 小程序业务接口响应体。
 * 与管理后台的 AjaxResult 分离，避免客户端同时兼容两套成功码和消息字段。
 */
public class MiniResult<T>
{
    private final int code;
    private final String message;
    private final T data;

    private MiniResult(int code, String message, T data)
    {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> MiniResult<T> success(T data)
    {
        return new MiniResult<>(0, "ok", data);
    }

    public static <T> MiniResult<T> error(int code, String message)
    {
        return new MiniResult<>(code, message, null);
    }

    public int getCode()
    {
        return code;
    }

    public String getMessage()
    {
        return message;
    }

    public T getData()
    {
        return data;
    }
}
