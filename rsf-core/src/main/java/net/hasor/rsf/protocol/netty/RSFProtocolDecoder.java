/*
 * Copyright 2008-2009 the original 赵永春(zyc@hasor.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hasor.rsf.protocol.netty;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import static net.hasor.rsf.domain.RSFConstants.RSF_Packet_Request;
import static net.hasor.rsf.domain.RSFConstants.RSF_Packet_Response;
import java.io.IOException;
import net.hasor.rsf.domain.ProtocolStatus;
import net.hasor.rsf.domain.RSFConstants;
import net.hasor.rsf.protocol.codec.Protocol;
import net.hasor.rsf.protocol.protocol.RequestSocketBlock;
import net.hasor.rsf.protocol.protocol.ResponseSocketBlock;
import net.hasor.rsf.utils.ProtocolUtils;
/**
 * 解码器
 * @version : 2014年10月10日
 * @author 赵永春(zyc@hasor.net)
 */
public class RSFProtocolDecoder extends LengthFieldBasedFrameDecoder {
    public RSFProtocolDecoder() {
        this(Integer.MAX_VALUE);
    }
    public RSFProtocolDecoder(int maxBodyLength) {
        // lengthFieldOffset   = 10
        // lengthFieldLength   = 3
        // lengthAdjustment    = 0
        // initialBytesToStrip = 0
        super(maxBodyLength, 10, 3, 0, 0);
    }
    //
    /*解码*/
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }
        //
        //* byte[1]  version RSF版本(0xC1)
        byte rsfHead = frame.getByte(0);
        short status = 0;
        //decode
        try {
            status = this.doDecode(rsfHead, ctx, frame);//协议解析
        } catch (Throwable e) {
            status = ProtocolStatus.ProtocolError;
        } finally {
            if (status == ProtocolStatus.OK)
                return null;
            /*错误情况*/
            frame = frame.resetReaderIndex().skipBytes(1);
            this.fireProtocolError(ctx, rsfHead, frame.readLong(), ProtocolStatus.ProtocolError);
        }
        return null;
    }
    protected ByteBuf extractFrame(ChannelHandlerContext ctx, ByteBuf buffer, int index, int length) {
        return buffer.slice(index, length);
    }
    //
    /**协议解析*/
    private short doDecode(byte rsfHead, ChannelHandlerContext ctx, ByteBuf frame) throws IOException {
        if ((RSF_Packet_Request | rsfHead) == rsfHead) {
            //request
            Protocol<RequestSocketBlock> requestProtocol = ProtocolUtils.requestProtocol(rsfHead);
            if (requestProtocol != null) {
                RequestSocketBlock block = requestProtocol.decode(frame);
                block.setReceiveTime(System.currentTimeMillis());
                ctx.fireChannelRead(block);
                return ProtocolStatus.OK;/*正常处理后返回*/
            }
        }
        if ((RSF_Packet_Response | rsfHead) == rsfHead) {
            //response
            Protocol<ResponseSocketBlock> responseProtocol = ProtocolUtils.responseProtocol(rsfHead);
            if (responseProtocol != null) {
                ResponseSocketBlock block = responseProtocol.decode(frame);
                block.setReceiveTime(System.currentTimeMillis());
                ctx.fireChannelRead(block);
                return ProtocolStatus.OK;/*正常处理后返回*/
            }
        }
        return ProtocolStatus.ProtocolError;
    }
    //
    /**发送错误 */
    private void fireProtocolError(ChannelHandlerContext ctx, byte rsfHead, long requestID, short status) {
        ResponseSocketBlock block = new ResponseSocketBlock();
        block.setHead(RSFConstants.RSF_Response);
        block.setRequestID(requestID);
        block.setStatus(status);
        block.setSerializeType(block.pushData("BlackHole".getBytes()));
        ctx.pipeline().writeAndFlush(block);
    }
}