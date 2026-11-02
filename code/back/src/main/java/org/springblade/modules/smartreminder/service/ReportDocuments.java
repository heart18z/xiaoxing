package org.springblade.modules.smartreminder.service;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;
import java.io.*;
import java.util.*;

@Component
public class ReportDocuments {
    public byte[] export(Map<String,Object> report,String format) throws IOException {
        String content=Objects.toString(report.get("markdown"),"");
        if(format.equals("docx"))return word(content);
        if(format.equals("pdf"))return pdf(content);
        throw new IllegalArgumentException("仅支持 Word 或 PDF");
    }
    private String plain(String line){return line.replaceFirst("^#{1,6}\\s*","").replace("**","");}
    private byte[] word(String content)throws IOException {
        try(var doc=new XWPFDocument();var bytes=new ByteArrayOutputStream()){
            for(String line:content.split("\n")){
                if(line.isBlank())continue;
                var p=doc.createParagraph();p.setSpacingAfter(140);p.setSpacingBetween(1.4);
                if(line.startsWith("#")){p.setSpacingBefore(200);p.setKeepNext(true);}
                var r=p.createRun();r.setFontFamily("Microsoft YaHei");r.setFontSize(line.startsWith("## ")?20:line.startsWith("### ")?15:11);
                r.setBold(line.startsWith("#"));r.setColor(line.startsWith("#")?"243D70":"374151");r.setText(plain(line));
            }
            doc.write(bytes);return bytes.toByteArray();
        }
    }
    private byte[] pdf(String content)throws IOException {
        try(var doc=new PDDocument();var bytes=new ByteArrayOutputStream();var fontInput=getClass().getResourceAsStream("/fonts/NotoSansSC-Regular.ttf")){
            if(fontInput==null)throw new IOException("报告字体缺失");
            var font=PDType0Font.load(doc,fontInput,true);PDPageContentStream stream=null;float y=0;
            try{
                for(String source:content.split("\n")){
                    if(source.isBlank()){y-=6;continue;}
                    float size=source.startsWith("## ")?19:source.startsWith("### ")?14:10.5f;
                    List<String> lines=new ArrayList<>();StringBuilder buffer=new StringBuilder();float width=0;
                    for(int cp:plain(source).codePoints().toArray()){
                        String symbol=new String(Character.toChars(cp));float w;
                        try{w=font.getStringWidth(symbol)*size/1000;}catch(IllegalArgumentException e){symbol="□";w=size;}
                        if(width+w>487&&buffer.length()>0){lines.add(buffer.toString());buffer.setLength(0);width=0;}
                        buffer.append(symbol);width+=w;
                    }
                    lines.add(buffer.toString());
                    for(String line:lines){
                        if(stream==null||y<62){if(stream!=null)stream.close();var page=new PDPage(PDRectangle.A4);doc.addPage(page);stream=new PDPageContentStream(doc,page);y=785;}
                        stream.beginText();stream.setFont(font,size);stream.setNonStrokingColor(36/255f,53/255f,80/255f);stream.newLineAtOffset(54,y);stream.showText(line);stream.endText();y-=size*1.65f;
                    }
                    y-=6;
                }
            }finally{if(stream!=null)stream.close();}
            doc.save(bytes);return bytes.toByteArray();
        }
    }
}
