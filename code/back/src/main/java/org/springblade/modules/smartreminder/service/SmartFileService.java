package org.springblade.modules.smartreminder.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SmartFileService {

	private static final long MAX_SIZE = 20L * 1024 * 1024;
	private static final int MAX_TEXT_LENGTH = 60000;
	private static final Set<String> ALLOWED = Set.of("txt", "md", "doc", "docx", "pdf", "xlsx", "png", "jpg", "jpeg", "webp", "gif");
	private static final Set<String> IMAGES=Set.of("png","jpg","jpeg","webp","gif");

	private final JdbcTemplate jdbcTemplate;
	private final NewApiClient aiClient;

	@Value("${smart-reminder.upload-dir:${user.dir}/uploads/smart-reminder}")
	private String uploadDir;

	public Map<String, Object> upload(MultipartFile file) {
		if (file == null || file.isEmpty()) throw new ServiceException("请选择文件");
		if (file.getSize() > MAX_SIZE) throw new ServiceException("文件大小不能超过20MB");
		String originalName = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
		String extension = extension(originalName);
		if (!ALLOWED.contains(extension)) {
			throw new ServiceException("支持图片 JPG/PNG/WEBP/GIF，以及 PDF、Word、TXT、MD、XLSX 文档");
		}
		if(IMAGES.contains(extension)&&file.getSize()>8L*1024*1024)throw new ServiceException("图片大小不能超过8MB");
		Long id = IdWorker.getId();
		String storedName = id + "." + extension;
		Path directory = Path.of(uploadDir, LocalDate.now().toString());
		Path target = directory.resolve(storedName).normalize();
		try {
			Files.createDirectories(directory);
			if (!target.startsWith(directory.normalize())) throw new ServiceException("非法文件路径");
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
			}
			String extracted = extract(target, extension);
			if(extracted.isBlank())throw new ServiceException("未提取到可读取文字；扫描版PDF请导出图片后上传识别");
			String status = "SUCCESS";
			String message = IMAGES.contains(extension)?"图片多模态解析完成":extracted.length()>=MAX_TEXT_LENGTH?"文档已解析，超长内容截取前60000字":"文档文字提取完成";
			jdbcTemplate.update("insert into blade_smart_file(id,user_id,original_name,stored_name,file_path,extension,file_size,extract_status,extracted_text,extract_message,create_time) values(?,?,?,?,?,?,?,?,?,?,now())",
				id, AuthUtil.getUserId(), originalName, storedName, target.toAbsolutePath().toString(), extension,
				file.getSize(), status, extracted, message);
			Map<String, Object> result = new LinkedHashMap<>();
			result.put("id", id.toString());
			result.put("kind",IMAGES.contains(extension)?"image":"document");
			result.put("name", originalName);
			result.put("extension", extension);
			result.put("size", file.getSize());
			result.put("extractStatus", status);
			result.put("extractMessage", message);
			result.put("preview", abbreviate(extracted, 300));
			return result;
		} catch (ServiceException e) {
			try { Files.deleteIfExists(target); } catch (Exception ignored) { }
			throw e;
		} catch (Exception e) {
			try { Files.deleteIfExists(target); } catch (Exception ignored) { }
			throw new ServiceException("文件保存或解析失败：" + e.getMessage());
		}
	}

	public String combinedText(List<Long> fileIds, Long userId) {
		if (fileIds == null || fileIds.isEmpty()) return "";
		if(fileIds.size()>6||new HashSet<>(fileIds).size()!=fileIds.size())throw new ServiceException("每条消息最多附加6个不同文件");
		String placeholders = String.join(",", Collections.nCopies(fileIds.size(), "?"));
		List<Object> args = new ArrayList<>(fileIds);
		args.add(userId);
		List<Map<String, Object>> files = jdbcTemplate.queryForList(
			"select original_name,extracted_text from blade_smart_file where id in (" + placeholders + ") and user_id=?",
			args.toArray());
		if(files.size()!=fileIds.size())throw new ServiceException("附件不存在或不属于当前用户，请重新上传");
		StringBuilder result = new StringBuilder();
		for (Map<String, Object> item : files) {
			result.append("\n【文件：").append(item.get("original_name")).append("】\n")
				.append(Objects.toString(item.get("extracted_text"), ""));
			if (result.length() >= MAX_TEXT_LENGTH) break;
		}
		return abbreviate(result.toString(), MAX_TEXT_LENGTH);
	}

	private String extract(Path path, String extension) throws Exception {
		return switch (extension) {
			case "txt", "md" -> extractText(path);
			case "doc" -> extractDoc(path);
			case "docx" -> extractDocx(path);
			case "xlsx" -> extractExcel(path);
			case "pdf" -> extractPdf(path);
			default -> IMAGES.contains(extension)?extractImage(path,extension):"";
		};
	}

	public List<String> fileNames(List<Long> fileIds,Long userId) {
		if(fileIds==null||fileIds.isEmpty())return List.of();
		List<Object> args=new ArrayList<>(fileIds);args.add(userId);
		return jdbcTemplate.query("select original_name from blade_smart_file where id in ("+String.join(",",Collections.nCopies(fileIds.size(),"?"))+") and user_id=?",(rs,n)->rs.getString(1),args.toArray());
	}

	private String extractText(Path path) throws Exception {
		byte[] bytes=Files.readAllBytes(path);
		java.nio.charset.Charset charset=StandardCharsets.UTF_8;
		if(bytes.length>=2&&bytes[0]==(byte)0xff&&bytes[1]==(byte)0xfe)charset=StandardCharsets.UTF_16LE;
		else if(bytes.length>=2&&bytes[0]==(byte)0xfe&&bytes[1]==(byte)0xff)charset=StandardCharsets.UTF_16BE;
		try{return abbreviate(charset.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString().replace("\uFEFF",""),MAX_TEXT_LENGTH);}
		catch(java.nio.charset.CharacterCodingException ex){return abbreviate(new String(bytes,java.nio.charset.Charset.forName("GB18030")),MAX_TEXT_LENGTH);}
	}
	private String extractDoc(Path path) throws Exception {
		try(InputStream input=Files.newInputStream(path);org.apache.poi.hwpf.HWPFDocument document=new org.apache.poi.hwpf.HWPFDocument(input);org.apache.poi.hwpf.extractor.WordExtractor extractor=new org.apache.poi.hwpf.extractor.WordExtractor(document)){
			return abbreviate(extractor.getText(),MAX_TEXT_LENGTH);
		}
	}
	private String extractImage(Path path,String extension) throws Exception {
		byte[] bytes=Files.readAllBytes(path);
		String detected=cn.hutool.core.io.FileTypeUtil.getType(new java.io.ByteArrayInputStream(bytes));
		if(!Set.of("jpg","jpeg","png","gif","webp").contains(Objects.toString(detected,"")))throw new ServiceException("文件内容不是有效图片，请重新选择");
		try(javax.imageio.stream.ImageInputStream input=javax.imageio.ImageIO.createImageInputStream(path.toFile())){
			var readers=javax.imageio.ImageIO.getImageReaders(input);
			if(readers.hasNext()){var reader=readers.next();try{reader.setInput(input);if((long)reader.getWidth(0)*reader.getHeight(0)>24000000)throw new ServiceException("图片尺寸过大，请压缩到2400万像素以内");}finally{reader.dispose();}}
		}
		try{return abbreviate(aiClient.describeImage(bytes,"image/"+(Set.of("jpg","jpeg").contains(detected)?"jpeg":detected)).content(),MAX_TEXT_LENGTH);}
		catch(Exception ex){throw new ServiceException("图片解析失败，请确认后台模型支持多模态图片输入后重试；文档上传不受影响");}
	}

	private String extractDocx(Path path) throws Exception {
		StringBuilder text = new StringBuilder();
		try (InputStream input = Files.newInputStream(path); XWPFDocument document = new XWPFDocument(input)) {
			for (XWPFParagraph paragraph : document.getParagraphs()) text.append(paragraph.getText()).append('\n');
			for (XWPFTable table : document.getTables()) {
				for (XWPFTableRow row : table.getRows()) {
					row.getTableCells().forEach(cell -> text.append(cell.getText()).append('\t'));
					text.append('\n');
				}
			}
		}
		return abbreviate(text.toString(), MAX_TEXT_LENGTH);
	}

	private String extractExcel(Path path) throws Exception {
		StringBuilder text = new StringBuilder();
		DataFormatter formatter = new DataFormatter(Locale.CHINA);
		try (InputStream input = Files.newInputStream(path); Workbook workbook = WorkbookFactory.create(input)) {
			for (Sheet sheet : workbook) {
				text.append("工作表：").append(sheet.getSheetName()).append('\n');
				for (Row row : sheet) {
					for (Cell cell : row) text.append(formatter.formatCellValue(cell)).append('\t');
					text.append('\n');
					if (text.length() >= MAX_TEXT_LENGTH) return abbreviate(text.toString(), MAX_TEXT_LENGTH);
				}
			}
		}
		return text.toString();
	}

	private String extractPdf(Path path) throws Exception {
		try (PDDocument document = Loader.loadPDF(path.toFile())) {
			return abbreviate(new PDFTextStripper().getText(document), MAX_TEXT_LENGTH);
		}
	}

	private String extension(String name) {
		int index = name.lastIndexOf('.');
		return index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT);
	}

	private String abbreviate(String text, int max) {
		if (text == null) return "";
		return text.length() <= max ? text : text.substring(0, max);
	}
}
