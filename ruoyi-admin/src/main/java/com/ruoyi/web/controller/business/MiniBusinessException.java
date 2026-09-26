package com.ruoyi.web.controller.business;

/** 小程序业务错误，统一由接口层转换为 code/message/data。 */
public class MiniBusinessException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    private final int code;

    public MiniBusinessException(int code, String message)
    {
        super(message);
        this.code = code;
    }

    public int getCode()
    {
        return code;
    }
}
