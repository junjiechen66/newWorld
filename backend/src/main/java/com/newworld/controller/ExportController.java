package com.newworld.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.newworld.common.Result;
import com.newworld.config.AuthInterceptor;
import com.newworld.dto.TaskQueryDTO;
import com.newworld.entity.Task;
import com.newworld.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

@Tag(name = "数据导出", description = "导出任务数据为 Excel")
@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private TaskService taskService;

    @Operation(summary = "导出任务为 Excel")
    @GetMapping("/tasks")
    public void exportTasks(TaskQueryDTO query, HttpServletResponse response) throws IOException {
        Long userId = AuthInterceptor.getCurrentUserId();
        List<Task> tasks = taskService.queryList(query, userId);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("任务导出", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), Task.class)
                .sheet("任务列表")
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .doWrite(tasks);
    }
}
