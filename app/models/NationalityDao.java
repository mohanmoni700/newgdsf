package models;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;

import java.util.List;

/**
 * Created by yaseen on 30-08-2016.
 */
public class NationalityDao {

    public static String getCodeForCountry(String country){

        String sql = "select n.three_letter_code from nationalities n where n.nationality = :country";

        SqlQuery sqlQuery = Ebean.createSqlQuery(sql);
        sqlQuery.setParameter("country", country);

        // execute the query returning a List of MapBean objects
        List<SqlRow> list = sqlQuery.findList();
        if(list.size() > 0){
            return  list.get(0).getString("three_letter_code");
        }
        return "";
    }
}
