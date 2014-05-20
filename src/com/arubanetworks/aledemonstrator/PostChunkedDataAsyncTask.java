package com.arubanetworks.aledemonstrator;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;


public class PostChunkedDataAsyncTask extends AsyncTask <String, Integer, String> {
	String TAG = "PostChunkedDataAsyncTask";
	String urlString = "/api/v1/survey/upstream";
	String testUrlString = "http://httpbin.org/post";
	int TIMEOUT_VALUE = 50000;
	String result;
	long timeStart = System.currentTimeMillis();
	long DURATION = 100000;

	protected String doInBackground(String... params) {
			int counter = 0;
			while(System.currentTimeMillis() - timeStart < DURATION){
				result = sendChunks();
				counter++;
				Log.v(TAG, "chunked counter "+counter+"  after "+(System.currentTimeMillis()-timeStart));
				if(counter == 1) { break;}
			}

		return result;
	}
	
	
	public void onPreExecute(){
		Log.i(TAG, "sendChunkedDataAsyncTask starting");
		MainActivity.sendChunkedDataAsyncTaskInProgress = true;
	}
	
	public void onPostExecute(String result){
		Log.i(TAG, "sendChunkedDataAsyncTask finished "+result);
		MainActivity.sendChunkedDataAsyncTaskInProgress = false;
	}
	
	private String sendChunks(){
		String result = "";
		String newText = text+text+text+text+text+text+text+text+text+text+text+text+text+text+text+text+text+text+text+text;
		//String newText = text;
		Log.v(TAG, "chunked newText length "+newText.length());
		char[] payload = newText.toCharArray();
		HttpURLConnection connection = null;
		try {
			try {
				URL url = new URL("http://" + MainActivity.aleHost +":" + MainActivity.alePort + urlString);
				//URL url = new URL(testUrlString);
				Log.d(TAG, "posting send chunked data "+url.toString());				
				connection = (HttpURLConnection) url.openConnection();
				connection.setConnectTimeout(TIMEOUT_VALUE);
				connection.setReadTimeout(TIMEOUT_VALUE);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("content-type", "text/plain; charset=UTF-8");
				connection.setDoOutput(true);
				connection.setDoInput(true);
				//connection.setFixedLengthStreamingMode(payload.length);
				connection.setRequestProperty("transfer-encoding", "chunked");
				connection.setChunkedStreamingMode(50);
				
				OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
				out.write(payload);
				out.flush();
				
				//Log.d(TAG, "chunked response header "+connection.getResponseCode()+"  "+connection.getResponseMessage()+"  length "+connection.getContentLength()+" type "+connection.getContentType());
				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
				StringBuilder builder = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					//Log.d(TAG, "chunked read line "+line);
		        	builder.append(line);	                
				}
				result = builder.toString();

				Log.d(TAG, "response to chunked data post \n"+result);
				out.close();
				in.close();            
			} finally {
				connection.disconnect();
			}
		} catch (Exception e) { Log.e(TAG, "Post Exception "+e); }
		return result;
	}
	
	
	static String text = "Gallia est omnis divisa in partes tres, quarum unam incolunt Belgae, aliam Aquitani, tertiam qui ipsorum lingua" +
			" Celtae, nostra Galli appellantur. Hi omnes lingua, institutis, legibus inter se differunt.  Gallos ab Aquitanis Garumna" +
			" flumen, a Belgis Matrona et Sequana dividit.  Horum omnium fortissimi sunt Belgae, propterea quod a cultu atque humanitate" +
			" provinciae longissime absunt, minimeque ad eos mercatores saepe commeant atque ea quae ad effeminandos animos pertinent" +
			" important, proximique sunt Germanis, qui trans Rhenum incolunt, quibuscum continenter bellum gerunt. Qua de causa Helvetii" +
			" quoque reliquos Gallos virtute praecedunt, quod fere cotidianis proeliis cum Germanis contendunt, cum aut suis finibus eos" +
			" prohibent aut ipsi in eorum finibus bellum gerunt.  Eorum una, pars, quam Gallos obtinere dictum est, initium capit a" +
			" flumine Rhodano, continetur Garumna flumine, Oceano, finibus Belgarum, attingit etiam ab Sequanis et Helvetiis flumen" +
			" Rhenum, vergit ad septentriones.  Belgae ab extremis Galliae finibus oriuntur, pertinent ad inferiorem partem fluminis" +
			" Rheni, spectant in septentrionem et orientem solem.  Aquitania a Garumna flumine ad Pyrenaeos montes et eam partem Oceani" +
			" quae est ad Hispaniam pertinet; spectat inter occasum solis et septentriones.  Apud Helvetios longe nobilissimus fuit et" +
			" ditissimus Orgetorix. Is M. Messala, et P. M. Pisone consulibus regni cupiditate inductus coniurationem nobilitatis fecit" +
			" et civitati persuasit ut de finibus suis cum omnibus copiis exirent: perfacile esse, cum virtute omnibus praestarent," +
			" totius Galliae imperio potiri. Id hoc facilius iis persuasit, quod undique loci natura Helvetii continentur: una ex parte" +
			" flumine Rheno latissimo atque altissimo, qui agrum Helvetium a Germanis dividit; altera ex parte monte Iura altissimo," +
			" qui est inter Sequanos et Helvetios; tertia lacu Lemanno et flumine Rhodano, qui provinciam nostram ab Helvetiis dividit." +
			" His rebus fiebat ut et minus late vagarentur et minus facile finitimis bellum inferre possent; qua ex parte homines" +
			" bellandi cupidi magno dolore adficiebantur. Pro multitudine autem hominum et pro gloria belli atque fortitudinis angustos" +
			" se fines habere arbitrabantur, qui in longitudinem milia passuum CCXL, in latitudinem CLXXX patebant.  His rebus adducti" +
			" et auctoritate Orgetorigis permoti constituerunt ea quae ad proficiscendum pertinerent comparare, iumentorum et carrorum" +
			" quam maximum numerum coemere, sementes quam maximas facere, ut in itinere copia frumenti suppeteret, cum proximis" +
			" civitatibus pacem et amicitiam confirmare. Ad eas res conficiendas biennium sibi satis esse duxerunt; in tertium annum" +
			" profectionem lege confirmant. Ad eas res conficiendas Orgetorix deligitur. Is sibi legationem ad civitates suscipit." +
			" In eo itinere persuadet Castico, Catamantaloedis filio, Sequano, cuius pater regnum in Sequanis multos annos obtinuerat" +
			" et a senatu populi Romani amicus appellatus erat, ut regnum in civitate sua occuparet, quod pater ante habuerit; itemque" +
			" Dumnorigi Haeduo, fratri Diviciaci, qui eo tempore principatum in civitate obtinebat ac maxime plebi acceptus erat, ut" +
			" idem conaretur persuadet eique filiam suam in matrimonium dat. Perfacile factu esse illis probat conata perficere," +
			" propterea quod ipse suae civitatis imperium obtenturus esset: non esse dubium quin totius Galliae plurimum Helvetii" +
			" possent; se suis copiis suoque exercitu illis regna conciliaturum confirmat. Hac oratione adducti inter se fidem et ius" +
			" iurandum dant et regno occupato per tres potentissimos ac firmissimos populos totius Galliae sese potiri posse sperant."+
			" Ea res est Helvetiis per indicium enuntiata. Moribus suis Orgetoricem ex vinculis causam dicere coegerunt; damnatum poenam" +
			" sequi oportebat, ut igni cremaretur. Die constituta causae dictionis Orgetorix ad iudicium omnem suam familiam, ad hominum" +
			" milia decem, undique coegit, et omnes clientes obaeratosque suos, quorum magnum numerum habebat, eodem conduxit; per eos ne" +
			" causam diceret se eripuit. Cum civitas ob eam rem incitata armis ius suum exequi conaretur multitudinemque hominum ex agris" +
			" magistratus cogerent, Orgetorix mortuus est; neque abest suspicio, ut Helvetii arbitrantur, quin ipse sibi mortem consciverit." +
			" Post eius mortem nihilo minus Helvetii id quod constituerant facere conantur, ut e finibus suis exeant. Ubi iam se ad eam rem" +
			" paratos esse arbitrati sunt, oppida sua omnia, numero ad duodecim, vicos ad quadringentos, reliqua privata aedificia incendunt;" +
			" frumentum omne, praeter quod secum portaturi erant, comburunt, ut domum reditionis spe sublata paratiores ad omnia pericula" +
			" subeunda essent; trium mensum molita cibaria sibi quemque domo efferre iubent. Persuadent Rauracis et Tulingis et Latobrigis" +
			" finitimis, uti eodem usi consilio oppidis suis vicisque exustis una cum iis proficiscantur, Boiosque, qui trans Rhenum" +
			" incoluerant et in agrum Noricum transierant Noreiamque oppugnabant, receptos ad se socios sibi adsciscunt. Erant omnino itinera" +
			" duo, quibus itineribus domo exire possent: unum per Sequanos, angustum et difficile, inter montem Iuram et flumen Rhodanum, vix" +
			" qua singuli carri ducerentur, mons autem altissimus impendebat, ut facile perpauci prohibere possent; alterum per provinciam" +
			" nostram, multo facilius atque expeditius, propterea quod inter fines Helvetiorum et Allobrogum, qui nuper pacati erant," +
			" Rhodanus fluit isque non nullis locis vado transitur. Extremum oppidum Allobrogum est proximumque Helvetiorum finibus Genava." +
			" Ex eo oppido pons ad Helvetios pertinet. Allobrogibus sese vel persuasuros, quod nondum bono animo in populum Romanum" +
			" viderentur, existimabant vel vi coacturos ut per suos fines eos ire paterentur. Omnibus rebus ad profectionem comparatis diem" +
			" dicunt, qua die ad ripam Rhodani omnes conveniant. Is dies erat a. d. V. Kal. Apr. L. Pisone, A. Gabinio consulibus. Caesari" +
			" cum id nuntiatum esset, eos per provincia nostram iter facere conari, maturat ab urbe proficisci et quam maximis potest" +
			" itineribus in Galliam ulteriorem contendit et ad Genavam pervenit. Provinciae toti quam maximum potest militum numerum imperat" +
			" (erat omnino in Gallia ulteriore legio una), pontem, qui erat ad Genavam, iubet rescindi. Ubi de eius aventu Helvetii certiores" +
			" facti sunt, legatos ad eum mittunt nobilissimos civitatis, cuius legationis Nammeius et Verucloetius principem locum obtinebant," +
			" qui dicerent sibi esse in animo sine ullo maleficio iter per provinciam facere, propterea quod aliud iter haberent nullum:" +
			" rogare ut eius voluntate id sibi facere liceat. Caesar, quod memoria tenebat L. Cassium consulem occisum exercitumque eius ab" +
			" Helvetiis pulsum et sub iugum missum, concedendum non putabat; neque homines inimico animo, data facultate per provinciam" +
			" itineris faciundi, temperaturos ab iniuria et maleficio existimabat. Tamen, ut spatium intercedere posset dum milites quos" +
			" imperaverat convenirent, legatis respondit diem se ad deliberandum sumpturum: si quid vellent, ad Id. April. reverterentur."+
			" Interea ea legione quam secum habebat militibusque, qui ex provincia convenerant, a lacu Lemanno, qui in flumen Rhodanum" +
			" influit, ad montem Iuram, qui fines Sequanorum ab Helvetiis dividit, milia passuum XVIIII murum in altitudinem pedum sedecim" +
			" fossamque perducit. Eo opere perfecto praesidia disponit, castella communit, quo facilius, si se invito transire conentur," +
			" prohibere possit. Ubi ea dies quam constituerat cum legatis venit et legati ad eum reverterunt, negat se more et exemplo" +
			" populi Romani posse iter ulli per provinciam dare et, si vim lacere conentur, prohibiturum ostendit. Helvetii ea spe deiecti" +
			" navibus iunctis ratibusque compluribus factis, alii vadis Rhodani, qua minima altitudo fluminis erat, non numquam interdiu," +
			" saepius noctu si perrumpere possent conati, operis munitione et militum concursu et telis repulsi, hoc conatu destiterunt." +
			" Relinquebatur una per Sequanos via, qua Sequanis invitis propter angustias ire non poterant. His cum sua sponte persuadere" +
			" non possent, legatos ad Dumnorigem Haeduum mittunt, ut eo deprecatore a Sequanis impetrarent. Dumnorix gratia et largitione" +
			" apud Sequanos plurimum poterat et Helvetiis erat amicus, quod ex ea civitate Orgetorigis filiam in matrimonium duxerat, et" +
			" cupiditate regni adductus novis rebus studebat et quam plurimas civitates suo beneficio habere obstrictas volebat. Itaque" +
			" rem suscipit et a Sequanis impetrat ut per fines suos Helvetios ire patiantur, obsidesque uti inter sese dent perficit:" +
			" Sequani, ne itinere Helvetios prohibeant, Helvetii, ut sine maleficio et iniuria transeant. Caesari renuntiatur Helvetiis" +
			" esse in animo per agrum Sequanorum et Haeduorum iter in Santonum fines facere, qui non longe a Tolosatium finibus absunt," +
			" quae civitas est in provincia. Id si fieret, intellegebat magno cum periculo provinciae futurum ut homines bellicosos, populi" +
			" Romani inimicos, locis patentibus maximeque frumentariis finitimos haberet. Ob eas causas ei munitioni quam fecerat" +
			" T. Labienum legatum praeficit; ipse in Italiam magnis itineribus contendit duasque ibi legiones conscribit et tres, quae" +
			" circum Aquileiam hiemabant, ex hibernis educit et, qua proximum iter in ulteriorem Galliam per Alpes erat, cum his quinque" +
			" legionibus ire contendit. Ibi Ceutrones et Graioceli et Caturiges locis superioribus occupatis itinere exercitum prohibere" +
			" conantur. Compluribus his proeliis pulsis ab Ocelo, quod est citerioris provinciae extremum, in fines Vocontiorum ulterioris" +
			" provinciae die septimo pervenit; inde in Allobrogum fines, ab Allobrogibus in Segusiavos exercitum ducit. Hi sunt extra" +
			" provinciam trans Rhodanum primi. Helvetii iam per angustias et fines Sequanorum suas copias traduxerant et in Haeduorum" +
			" fines pervenerant eorumque agros populabantur. Haedui, cum se suaque ab iis defendere non possent, legatos ad Caesarem" +
			" mittunt rogatum auxilium: ita se omni tempore de populo Romano meritos esse ut paene in conspectu exercitus nostri agri" +
			" vastari, liberi [eorum] in servitutem abduci, oppida expugnari non debuerint. Eodem tempore Haedui Ambarri, necessarii et" +
			" consanguinei Haeduorum, Caesarem certiorem faciunt sese depopulatis agris non facile ab oppidis vim hostium prohibere. Item" +
			" Allobroges, qui trans Rhodanum vicos possessionesque habebant, fuga se ad Caesarem recipiunt et demonstrant sibi praeter" +
			" agri solum nihil esse reliqui. Quibus rebus adductus Caesar non expectandum sibi statuit dum, omnibus, fortunis sociorum" +
			" consumptis, in Santonos Helvetii pervenirent. Flumen est Arar, quod per fines Haeduorum et Sequanorum in Rhodanum influit," +
			" incredibili lenitate, ita ut oculis in utram partem fluat iudicari non possit. Id Helvetii ratibus ac lintribus iunctis" +
			" transibant. Ubi per exploratores Caesar certior factus est tres iam partes copiarum Helvetios id flumen traduxisse, quartam" +
			" vero partem citra flumen Ararim reliquam esse, de tertia vigilia cum legionibus tribus e castris profectus ad eam partem" +
			" pervenit quae nondum flumen transierat. Eos impeditos et inopinantes adgressus magnam partem eorum concidit; reliqui sese" +
			" fugae mandarunt atque in proximas silvas abdiderunt. Is pagus appellabatur Tigurinus; nam omnis civitas Helvetia in quattuor" +
			" pagos divisa est. Hic pagus unus, cum domo exisset, patrum nostrorum memoria L. Cassium consulem interfecerat et eius" +
			" exercitum sub iugum miserat. Ita sive casu sive consilio deorum immortalium quae pars civitatis Helvetiae insignem" +
			" calamitatem populo Romano intulerat, ea princeps poenam persolvit. Qua in re Caesar non solum publicas, sed etiam privatas" +
			" iniurias ultus est, quod eius soceri L. Pisonis avum, L. Pisonem legatum, Tigurini eodem proelio quo Cassium interfecerant." +
			" Hoc proelio facto, reliquas copias Helvetiorum ut consequi posset, pontem in Arari faciendum curat atque ita exercitum" +
			" traducit. Helvetii repentino eius adventu commoti cum id quod ipsi diebus XX aegerrime confecerant, ut flumen transirent," +
			" illum uno die fecisse intellegerent, legatos ad eum mittunt; cuius legationis Divico princeps fuit, qui bello Cassiano dux" +
			" Helvetiorum fuerat. Is ita cum Caesare egit: si pacem populus Romanus cum Helvetiis faceret, in eam partem ituros atque ibi" +
			" futuros Helvetios ubi eos Caesar constituisset atque esse voluisset; sin bello persequi perseveraret, reminisceretur et" +
			" veteris incommodi populi Romani et pristinae virtutis Helvetiorum. Quod improviso unum pagum adortus esset, cum ii qui flumen" +
			" transissent suis auxilium ferre non possent, ne ob eam rem aut suae magnopere virtuti tribueret aut ipsos despiceret. Se ita" +
			" a patribus maioribusque suis didicisse, ut magis virtute contenderent quam dolo aut insidiis niterentur. Quare ne committeret" +
			" ut is locus ubi constitissent ex calamitate populi Romani et internecione exercitus nomen caperet aut memoriam proderet.";
	
	
}
