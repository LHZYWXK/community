package hk.hku.cs.community.event;

import com.alibaba.fastjson.JSONObject;
import hk.hku.cs.community.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {
    @Autowired
    private KafkaTemplate kafkaTemplate;

    // 处理时间：发消息
    public void fireEvent(Event event) {
        // 将时间发布到指定的主题
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
}
