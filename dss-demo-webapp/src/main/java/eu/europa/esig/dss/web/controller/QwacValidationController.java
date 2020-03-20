package eu.europa.esig.dss.web.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;

import eu.europa.esig.dss.CertificateReorderer;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.service.http.commons.SSLCertificateLoader;
import eu.europa.esig.dss.spi.x509.CommonCertificateSource;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.CertificateValidator;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.reports.CertificateReports;
import eu.europa.esig.dss.web.model.QwacValidationForm;

@Controller
@SessionAttributes({ "simpleReportXml", "detailedReportXml", "diagnosticDataXml" })
@RequestMapping(value = "/qwac-validation")
public class QwacValidationController extends AbstractValidationController {

	private static final Logger LOG = LoggerFactory.getLogger(QwacValidationController.class);

	private static final String VALIDATION_TILE = "qwac-validation";
	private static final String VALIDATION_RESULT_TILE = "validation-result";
	
	@Autowired
	protected SSLCertificateLoader sslCertificateLoader;

	@RequestMapping(method = RequestMethod.GET)
	public String showValidationForm(Model model, HttpServletRequest request) {
		QwacValidationForm qwacValidationForm = new QwacValidationForm();
		model.addAttribute("qwacValidationForm", qwacValidationForm);
		return VALIDATION_TILE;
	}

	@RequestMapping(method = RequestMethod.POST)
	public String validate(@ModelAttribute("qwacValidationForm") @Valid QwacValidationForm qwacValidationForm, BindingResult result, Model model) {
		if (result.hasErrors()) {
			if (LOG.isDebugEnabled()) {
				List<ObjectError> allErrors = result.getAllErrors();
				for (ObjectError error : allErrors) {
					LOG.debug(error.getDefaultMessage());
				}
			}
			return VALIDATION_TILE;
		}

		String url = qwacValidationForm.getUrl();
		
		LOG.info("Start QWAC validation. Requested URL : '{}'", url);

		List<CertificateToken> certificates = sslCertificateLoader.getCertificates(url);
		
		if (Utils.isCollectionNotEmpty(certificates)) {
			CertificateReorderer certificateReorderer = new CertificateReorderer(certificates);
			List<CertificateToken> certificateChain = certificateReorderer.getOrderedCertificates();
			CertificateToken qwacCertificate = certificateChain.iterator().next();
			
			CertificateVerifier cv = certificateVerifier;
	        cv.setIncludeCertificateTokenValues(qwacValidationForm.isIncludeCertificateTokens());
	        cv.setIncludeCertificateRevocationValues(qwacValidationForm.isIncludeRevocationTokens());
	        
	        CommonCertificateSource commonCertificateSource = new CommonCertificateSource();
	        for (CertificateToken certificateToken : certificateChain) {
	        	commonCertificateSource.addCertificate(certificateToken);
	        }
	        cv.setAdjunctCertSource(commonCertificateSource);
			
			CertificateValidator certificateValidator = CertificateValidator.fromCertificate(qwacCertificate);
			certificateValidator.setCertificateVerifier(certificateVerifier);

			CertificateReports reports = certificateValidator.validate();
			
			setAttributesModels(model, reports);

			LOG.info("End certificate validation");

			return VALIDATION_RESULT_TILE;
		}

		throw new DSSException(String.format("The requested URL '%s' did not return a list of certificates to validate.", url));
	}

}
