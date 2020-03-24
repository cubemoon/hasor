var queryMap = {
    "mysql"  : @@inner_dataway_sql()<% select * from interface_info order by api_create_time asc; %>,
    "pg"     : @@inner_dataway_sql()<% s %>,
    "oracle" : @@inner_dataway_sql()<% s %>
};

return queryMap[`net.hasor.dataway.config.DataBaseType`]() => [
    {
        "id"      : api_id,
        "checked" : false,
        "select"  : api_method,
        "path"    : api_path,
        "status"  : api_status,
        "comment" : api_comment
    }
];