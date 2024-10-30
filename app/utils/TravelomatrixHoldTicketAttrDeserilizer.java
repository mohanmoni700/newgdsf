package utils;

import com.compassites.model.travelomatrix.ResponseModels.HoldTicket.Attr;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class TravelomatrixHoldTicketAttrDeserilizer extends JsonDeserializer<Attr> {
        @Override
        public Attr deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            Attr attr = new Attr();

            // Check if the node is a text value (string)
            if (node.isTextual()) {
                String value = node.asText();
               // Set the field accordingly
                // You might set field2 or leave it null based on your logic
            }
            // Check if the node is an object
            else if (node.isObject()) {
                JsonNode availableSeats = node.get("AvailableSeats");
                JsonNode baggage = node.get("Baggage");
                JsonNode cabinBaggage = node.get("CabinBaggage");

                if (availableSeats != null) {
                    attr.setAvailableSeats(availableSeats.asText());
                }
                if (baggage != null) {
                    attr.setBaggage(baggage.asText());
                }
                if (cabinBaggage != null) {
                    attr.setCabinBaggage(cabinBaggage.asText());
                }
            }

            return attr;
        }

}
