package com.seattleacademy.team20;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

@Controller
public class SkillController {

	private static final Logger logger = LoggerFactory.getLogger(SkillController.class);
	@Autowired
	private JdbcTemplate jdbcTemplate;

	// MySQLと接続するため
	/**
	 * Simply selects the home view to render by returning its name.
	 * @throws IOException
	 */
	@RequestMapping(value = "/skillUpload", method = RequestMethod.GET)
	public String skillUpload(Locale locale, Model model) {
		logger.info("Welcome home! The client locale is {}.", locale);

		try {
			initialize();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		List<Skill> skills = selectSkills();
		uploadSkill(skills);

		return "skillUpload";
	}

	//Listの宣言
	public List<Skill> selectSkills() {
		final String sql = "select * from skill_categories";
		return jdbcTemplate.query(sql, new RowMapper<Skill>() {

			public Skill mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new Skill(rs.getString("category"),
						rs.getString("name"), rs.getInt("score"));
			}
		});
	}

	//Fetch the service account key JSON file contents
	//SDKの初期化
	public void initialize() throws IOException {
		FileInputStream refreshToken = new FileInputStream(
				"/Users/nao/academy20/devo-portfolio-firebase-adminsdk-5izgr-676f45dcf2.json");
		FirebaseOptions options = new FirebaseOptions.Builder()
				.setCredentials(GoogleCredentials.fromStream(refreshToken))
				.setDatabaseUrl("https://devo-portfolio.firebaseio.com/")
				.build();
		FirebaseApp.initializeApp(options);
	}

	public void uploadSkill(List<Skill> skills) {
		final FirebaseDatabase database = FirebaseDatabase.getInstance();
		DatabaseReference ref = database.getReference("skill-categories");
		//Dataの取得（MySQLから）
		//Dataを取得してから形成
		//Databaseにアップロードする
		//JSPにわたすデータを設定する
		List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
		Map<String, Object> map;
		Map<String, List<Skill>> skillMap = skills.stream().collect(Collectors.groupingBy(Skill::getCategory));
		for (Map.Entry<String, List<Skill>> entry : skillMap.entrySet()) {
			//		    System.out.println(entry.getKey());
			//		    System.out.println(entry.getValue());
			map = new HashMap<>();
			map.put("category", entry.getKey());
			map.put("skills", entry.getValue());

			dataList.add(map);
		}
		//リアルタイムデータベース更新
		ref.setValue(dataList, new DatabaseReference.CompletionListener() {
			@Override
			public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
				if (databaseError != null) {
					System.out.println("Data could be saved" + databaseError.getMessage());
				} else {
					System.out.println("Data save successfully.");
				}
			}
		});
	}

	//  ↓メインクラス名
	public class Skill {
		private String category;
		private String name;
		private int score;

		public Skill(String category, String name, int score) {
			this.category = category;
			this.name = name;
			this.score = score;
		}

		public String getCategory() {
			return category;
		}

		public String getName() {
			return name;
		}

		public int getScore() {
			return score;
		}
		//Map<String, List<Skill>> grpByType = Skill.stream().collect(
		// Collectors.groupingBy(Skill::getSkillCategory));
	}
}

//As an admin, the app has access to read and write all data, regardless of Security Rules
//DatabaseReference ref = FirebaseDatabase.getInstance()
//.getReference("restricted_access/secret_document");
//ref.addListenerForSingleValueEvent(new ValueEventListener() {
//@Override
//public void onDataChange(DataSnapshot dataSnapshot) {
//Object document = dataSnapshot.getValue();
//System.out.println(document);
//}
